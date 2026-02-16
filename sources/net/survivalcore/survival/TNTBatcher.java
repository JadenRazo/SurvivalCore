package net.survivalcore.survival;

import net.survivalcore.config.SurvivalCoreConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Batch TNT detonations during tick, process at tick end.
 *
 * Groups explosions within a configurable radius and combines them
 * into a single explosion with averaged position and scaled power.
 * This dramatically improves performance for TNT cannons and large
 * TNT arrays.
 *
 * Combined power is calculated as sqrt(sum of squares) to avoid
 * overpowered explosions from batching.
 */
public final class TNTBatcher {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static TNTBatcher instance;

    private final boolean enabled;
    private final double groupRadiusSq;
    private final List<PendingDetonation> queue = new ArrayList<>(64);
    private final AtomicInteger batchedCount = new AtomicInteger(0);

    private TNTBatcher(SurvivalCoreConfig config) {
        this.enabled = config.tntBatchingEnabled;
        this.groupRadiusSq = config.tntGroupRadius * config.tntGroupRadius;
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        instance = new TNTBatcher(config);

        if (instance.enabled) {
            LOGGER.info("TNT batching enabled: group radius " + config.tntGroupRadius + " blocks");
        } else {
            LOGGER.info("TNT batching disabled");
        }
    }

    public static TNTBatcher get() {
        if (instance == null) {
            throw new IllegalStateException("TNTBatcher not initialized");
        }
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    /**
     * Queue a TNT detonation for batch processing.
     *
     * @param x     explosion X position
     * @param y     explosion Y position
     * @param z     explosion Z position
     * @param power explosion power (typically 4.0 for TNT)
     */
    public void queueDetonation(double x, double y, double z, float power) {
        if (!enabled) return;
        queue.add(new PendingDetonation(x, y, z, power));
    }

    /**
     * Process all batched explosions, grouping nearby detonations.
     * Should be called at the end of each tick.
     *
     * @param explosionCallback callback to execute actual explosion
     */
    public void processBatch(ExplosionCallback explosionCallback) {
        if (!enabled || queue.isEmpty()) {
            queue.clear();
            return;
        }

        List<PendingDetonation> remaining = new ArrayList<>(queue);
        queue.clear();

        while (!remaining.isEmpty()) {
            PendingDetonation current = remaining.remove(0);
            List<PendingDetonation> group = new ArrayList<>();
            group.add(current);

            // Find all detonations within groupRadius
            remaining.removeIf(other -> {
                double dx = other.x - current.x;
                double dy = other.y - current.y;
                double dz = other.z - current.z;
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq <= groupRadiusSq) {
                    group.add(other);
                    return true;
                }
                return false;
            });

            // Process the group
            if (group.size() == 1) {
                // Single explosion, no batching
                PendingDetonation det = group.get(0);
                explosionCallback.explode(det.x, det.y, det.z, det.power);
            } else {
                // Batch multiple explosions
                double avgX = 0, avgY = 0, avgZ = 0;
                double powerSumSq = 0;

                for (PendingDetonation det : group) {
                    avgX += det.x;
                    avgY += det.y;
                    avgZ += det.z;
                    powerSumSq += det.power * det.power;
                }

                avgX /= group.size();
                avgY /= group.size();
                avgZ /= group.size();
                float combinedPower = (float) Math.sqrt(powerSumSq);

                explosionCallback.explode(avgX, avgY, avgZ, combinedPower);
                batchedCount.addAndGet(group.size() - 1);
            }
        }
    }

    /**
     * Get the number of explosions batched and reset counter.
     *
     * @return count of batched explosions
     */
    public int getBatchedCount() {
        if (!enabled) return 0;
        return batchedCount.getAndSet(0);
    }

    /**
     * Get current stats for monitoring.
     */
    public BatchStats getStats() {
        return new BatchStats(
            queue.size(),
            groupRadiusSq,
            batchedCount.get()
        );
    }

    private record PendingDetonation(double x, double y, double z, float power) {}

    /**
     * Callback interface for executing actual explosions.
     */
    @FunctionalInterface
    public interface ExplosionCallback {
        void explode(double x, double y, double z, float power);
    }

    public record BatchStats(int queuedExplosions, double groupRadiusSq, int batchedCount) {}
}
