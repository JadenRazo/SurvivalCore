package net.survivalcore.survival;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.survivalcore.config.SurvivalCoreConfig;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Deduplicate scheduled ticks for the same position within the same tick.
 *
 * Prevents redundant block ticks when multiple sources schedule updates
 * for the same position. Common in redstone contraptions where the same
 * block receives updates from multiple neighbors.
 *
 * Reset at the start of each tick to allow normal scheduling in subsequent ticks.
 */
public final class TickCoalescer {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static TickCoalescer instance;

    private final boolean enabled;
    private final LongOpenHashSet scheduledThisTick = new LongOpenHashSet();
    private final AtomicInteger coalescedCount = new AtomicInteger(0);

    private TickCoalescer(SurvivalCoreConfig config) {
        // Note: This feature doesn't have a dedicated config flag yet,
        // it's enabled by default as part of block tick optimizations.
        // For now, we'll enable it if any performance optimizations are on.
        this.enabled = config.fastMathEnabled; // Use as proxy for perf opts enabled
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        instance = new TickCoalescer(config);

        if (instance.enabled) {
            LOGGER.info("Tick coalescing enabled");
        } else {
            LOGGER.info("Tick coalescing disabled");
        }
    }

    public static TickCoalescer get() {
        if (instance == null) {
            throw new IllegalStateException("TickCoalescer not initialized");
        }
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    /**
     * Check if a block at the given position should be ticked.
     * Returns false if this position was already scheduled this tick.
     *
     * @param blockPosLong packed block position (x, y, z)
     * @return true if this position should tick, false if duplicate
     */
    public boolean shouldTick(long blockPosLong) {
        if (!enabled) return true;

        boolean added = scheduledThisTick.add(blockPosLong);
        if (!added) {
            coalescedCount.incrementAndGet();
        }
        return added;
    }

    /**
     * Reset the scheduled tick set at the start of each tick.
     */
    public void reset() {
        if (enabled) {
            scheduledThisTick.clear();
        }
    }

    /**
     * Get the number of coalesced (deduplicated) ticks and reset counter.
     *
     * @return count of coalesced ticks
     */
    public int getCoalescedCount() {
        if (!enabled) return 0;
        return coalescedCount.getAndSet(0);
    }

    /**
     * Get current stats for monitoring.
     */
    public CoalescerStats getStats() {
        return new CoalescerStats(
            scheduledThisTick.size(),
            coalescedCount.get()
        );
    }

    public record CoalescerStats(int uniquePositions, int coalescedCount) {}
}
