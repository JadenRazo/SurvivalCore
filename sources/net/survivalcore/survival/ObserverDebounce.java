package net.survivalcore.survival;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.survivalcore.config.SurvivalCoreConfig;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Collapse rapid observer triggers within a configurable window.
 *
 * Observers in redstone contraptions can trigger excessively, causing
 * unnecessary block updates and lag. This system debounces rapid triggers,
 * allowing only one fire event per configurable interval.
 *
 * Periodically cleans up stale entries to prevent unbounded memory growth.
 */
public final class ObserverDebounce {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static ObserverDebounce instance;

    private final boolean enabled;
    private final int minIntervalTicks;
    private final Long2LongOpenHashMap lastTriggerTick = new Long2LongOpenHashMap();
    private final AtomicInteger debouncedCount = new AtomicInteger(0);
    private long lastCleanupTick = 0;

    private ObserverDebounce(SurvivalCoreConfig config) {
        this.enabled = config.observerDebounceEnabled;
        this.minIntervalTicks = config.observerDebounceMinIntervalTicks;
        this.lastTriggerTick.defaultReturnValue(-1000L);
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        instance = new ObserverDebounce(config);

        if (instance.enabled) {
            LOGGER.info("Observer debounce enabled: min interval " + config.observerDebounceMinIntervalTicks + " ticks");
        } else {
            LOGGER.info("Observer debounce disabled");
        }
    }

    public static ObserverDebounce get() {
        if (instance == null) {
            throw new IllegalStateException("ObserverDebounce not initialized");
        }
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    /**
     * Check if an observer at the given position should fire.
     *
     * @param blockPosLong packed block position (x, y, z)
     * @param currentTick  current server tick
     * @return true if observer should fire, false if debounced
     */
    public boolean shouldFire(long blockPosLong, long currentTick) {
        if (!enabled) return true;

        long lastTick = lastTriggerTick.get(blockPosLong);
        if (currentTick - lastTick < minIntervalTicks) {
            debouncedCount.incrementAndGet();
            return false;
        }

        lastTriggerTick.put(blockPosLong, currentTick);
        return true;
    }

    /**
     * Periodic cleanup of stale entries to prevent unbounded growth.
     * Should be called approximately every ~1200 ticks (1 minute).
     *
     * @param currentTick current server tick
     */
    public void cleanup(long currentTick) {
        if (!enabled) return;

        // Cleanup every 1200 ticks
        if (currentTick - lastCleanupTick < 1200) return;
        lastCleanupTick = currentTick;

        // Remove entries older than 10 seconds (200 ticks)
        long staleThreshold = currentTick - 200;
        lastTriggerTick.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < staleThreshold);
    }

    /**
     * Get the number of debounced observer triggers this tick and reset counter.
     *
     * @return count of debounced triggers
     */
    public int getDebouncedCount() {
        if (!enabled) return 0;
        return debouncedCount.getAndSet(0);
    }

    /**
     * Get current stats for monitoring.
     */
    public DebounceStats getStats() {
        return new DebounceStats(
            lastTriggerTick.size(),
            minIntervalTicks,
            debouncedCount.get()
        );
    }

    public record DebounceStats(int trackedObservers, int minIntervalTicks, int debouncedThisTick) {}
}
