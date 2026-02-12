package net.survivalcore.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Manages async mob spawn calculations.
 *
 * Phase 1 (async): Calculate spawn positions, check light/biome/caps using chunk snapshots.
 * Phase 2 (main thread): Create entity instances to preserve plugin compatibility.
 *
 * This split keeps the expensive position/condition checking off the main thread
 * while ensuring entity creation events fire on the main thread as plugins expect.
 */
public final class AsyncMobSpawner {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static volatile AsyncMobSpawner instance;
    private final ExecutorService executor;

    private AsyncMobSpawner() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SurvivalCore-MobSpawner-" + count.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 2);
                return t;
            }
        };

        this.executor = new ThreadPoolExecutor(
            1, 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            factory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        LOGGER.info("Async mob spawner initialized");
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        if (!config.asyncMobSpawningEnabled) {
            LOGGER.info("Async mob spawning is disabled");
            return;
        }

        instance = new AsyncMobSpawner();
    }

    public static AsyncMobSpawner get() {
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    /**
     * Submit spawn position calculations to run asynchronously.
     */
    public void submit(Runnable task) {
        executor.execute(task);
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
