package net.survivalcore.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Manages the async entity tracking thread pool.
 *
 * Entity position/velocity/metadata broadcasts are moved off the main tick thread
 * to dedicated worker threads. This significantly reduces main thread load when
 * many entities are present.
 *
 * NPC-type entities (from Citizens, FancyNpcs, etc.) can optionally remain on
 * the main thread via compat-mode to prevent visibility glitches.
 */
public final class AsyncEntityTracker {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static volatile AsyncEntityTracker instance;
    private final ExecutorService executor;
    private final boolean compatMode;

    private AsyncEntityTracker(int threads, boolean compatMode) {
        this.compatMode = compatMode;

        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SurvivalCore-EntityTracker-" + count.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        };

        this.executor = new ThreadPoolExecutor(
            threads, threads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(4096),
            factory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        LOGGER.info("Async entity tracker initialized with " + threads + " threads"
            + (compatMode ? " (compat-mode enabled)" : ""));
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        if (!config.asyncEntityTrackerEnabled) {
            LOGGER.info("Async entity tracker is disabled");
            return;
        }

        int threads = config.resolveEntityTrackerThreads();
        instance = new AsyncEntityTracker(threads, config.asyncEntityTrackerCompatMode);
    }

    public static AsyncEntityTracker get() {
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    /**
     * Submit an entity tracking update to run asynchronously.
     * Falls back to synchronous execution if the queue is full (CallerRunsPolicy).
     */
    public void submit(Runnable task) {
        executor.execute(task);
    }

    /**
     * Whether NPC entities should be tracked synchronously for compatibility.
     */
    public boolean isCompatMode() {
        return compatMode;
    }

    /**
     * Check if an entity class name indicates an NPC that needs sync tracking.
     * This is a heuristic check based on common NPC plugin patterns.
     */
    public boolean requiresSyncTracking(String entityClassName) {
        if (!compatMode) return false;
        // Common NPC plugin entity class patterns
        return entityClassName.contains("NPC")
            || entityClassName.contains("Citizens")
            || entityClassName.contains("FancyNpc")
            || entityClassName.contains("ZNpc");
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
