package net.survivalcore.survival;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.survivalcore.config.SurvivalCoreConfig;
import net.survivalcore.monitoring.PerformanceMonitor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Per-chunk redstone update density tracking with tiered throttling.
 *
 * Counts redstone updates per chunk per tick and applies a tick divisor
 * when thresholds are crossed. Uses position hashing for fair staggering
 * of which updates get skipped.
 *
 * Three thresholds (updates per chunk per tick):
 * - Soft (64): divisor 2 - 50% of updates proceed
 * - Hard (150): divisor 4 - 25% proceed
 * - Critical (300): divisor 8 - 12.5% proceed
 *
 * Normal gameplay (clocks ~1-4/tick, piston doors ~20-40, sorting systems ~10-30)
 * stays well below the soft threshold.
 */
public final class RedstoneThrottler {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static RedstoneThrottler instance;

    private final boolean enabled;
    private final int softThreshold;
    private final int hardThreshold;
    private final int criticalThreshold;
    private final boolean alertAdmins;

    private final Long2IntOpenHashMap chunkUpdateCounts = new Long2IntOpenHashMap();
    private long lastAlertTick = 0;

    private RedstoneThrottler(SurvivalCoreConfig config) {
        this.enabled = config.redstoneChunkThrottleEnabled;
        this.softThreshold = config.redstoneChunkThrottleSoft;
        this.hardThreshold = config.redstoneChunkThrottleHard;
        this.criticalThreshold = config.redstoneChunkThrottleCritical;
        this.alertAdmins = config.redstoneChunkThrottleAlertAdmins;
        this.chunkUpdateCounts.defaultReturnValue(0);
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        instance = new RedstoneThrottler(config);

        if (instance.enabled) {
            LOGGER.info("Redstone chunk throttle enabled: soft=" + config.redstoneChunkThrottleSoft
                + ", hard=" + config.redstoneChunkThrottleHard
                + ", critical=" + config.redstoneChunkThrottleCritical);
        } else {
            LOGGER.info("Redstone chunk throttle disabled");
        }
    }

    public static RedstoneThrottler get() {
        if (instance == null) {
            throw new IllegalStateException("RedstoneThrottler not initialized");
        }
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    /**
     * Reset chunk update counts at the start of each tick.
     */
    public void resetCounts() {
        if (enabled) {
            chunkUpdateCounts.clear();
        }
    }

    /**
     * Track a redstone update in the given chunk.
     * Always called, even when the update will be throttled,
     * so density tracking remains accurate.
     *
     * @param chunkKey chunk coordinate key (ChunkPos.asLong)
     */
    public void trackUpdate(long chunkKey) {
        if (enabled) {
            chunkUpdateCounts.addTo(chunkKey, 1);
        }
    }

    /**
     * Determine whether a redstone update should proceed.
     * Uses position hashing with tick offset for fair staggering.
     *
     * @param chunkKey     chunk coordinate key
     * @param blockPosLong packed block position
     * @param currentTick  current server tick
     * @return true if the update should proceed, false to suppress
     */
    public boolean shouldUpdate(long chunkKey, long blockPosLong, long currentTick) {
        if (!enabled) return true;

        int divisor = getTickDivisor(chunkKey);
        if (divisor == 1) return true;

        // Stagger using position hash + tick for fairness
        long posHash = blockPosLong * 6364136223846793005L + 1442695040888963407L;
        if ((currentTick + posHash) % divisor != 0) {
            // Increment throttled counter in performance monitor
            if (PerformanceMonitor.isEnabled()) {
                PerformanceMonitor.get().incrementCounter(PerformanceMonitor.REDSTONE_THROTTLED);
            }
            return false;
        }
        return true;
    }

    /**
     * Get the tick divisor for a chunk based on redstone update density.
     *
     * Returns 1 (every tick), 2 (every other), 4 (quarter), or 8 (eighth).
     *
     * @param chunkKey chunk coordinate key
     * @return tick divisor
     */
    public int getTickDivisor(long chunkKey) {
        if (!enabled) return 1;

        int count = chunkUpdateCounts.get(chunkKey);
        if (count >= criticalThreshold) return 8;
        if (count >= hardThreshold) return 4;
        if (count >= softThreshold) return 2;
        return 1;
    }

    /**
     * Check and log alerts for redstone hotspot chunks.
     * Rate-limited to once per 1200 ticks (1 minute).
     *
     * @param currentTick current server tick
     */
    public void checkAlerts(long currentTick) {
        if (!enabled || !alertAdmins) return;

        if (currentTick - lastAlertTick < 1200) return;

        Map<Long, Integer> hotspots = getHotspots();
        if (hotspots.isEmpty()) return;

        int criticalCount = 0;
        int hardCount = 0;
        int softCount = 0;

        for (int count : hotspots.values()) {
            if (count >= criticalThreshold) criticalCount++;
            else if (count >= hardThreshold) hardCount++;
            else softCount++;
        }

        if (criticalCount > 0 || hardCount > 0) {
            LOGGER.warning(String.format(
                "Redstone hotspots detected: %d critical, %d hard, %d soft (total %d chunks)",
                criticalCount, hardCount, softCount, hotspots.size()
            ));
            lastAlertTick = currentTick;
        }
    }

    /**
     * Get a snapshot of chunks above the soft threshold.
     *
     * @return map of chunk key to update count
     */
    public Map<Long, Integer> getHotspots() {
        if (!enabled) return Map.of();

        Map<Long, Integer> hotspots = new HashMap<>();
        chunkUpdateCounts.forEach((chunkKey, count) -> {
            if (count >= softThreshold) {
                hotspots.put(chunkKey, count);
            }
        });
        return hotspots;
    }

    /**
     * Get detailed stats for a specific chunk.
     */
    public ChunkStats getChunkStats(long chunkKey) {
        if (!enabled) return ChunkStats.NONE;
        int count = chunkUpdateCounts.get(chunkKey);
        int divisor = getTickDivisor(chunkKey);
        return new ChunkStats(count, divisor);
    }

    public record ChunkStats(int updateCount, int tickDivisor) {
        public static final ChunkStats NONE = new ChunkStats(0, 1);
    }
}
