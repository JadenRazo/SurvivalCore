package net.survivalcore.async;

import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Entity AI optimization utilities.
 *
 * Provides distance-based tick frequency for entity AI,
 * goal selector throttling for inactive entities, and
 * brain tick batching for CPU cache optimization.
 */
public final class EntityAIOptimizer {

    // Distance thresholds (squared, to avoid sqrt)
    private static final double NEAR_DISTANCE_SQ = 32.0 * 32.0;     // 32 blocks
    private static final double MEDIUM_DISTANCE_SQ = 64.0 * 64.0;   // 64 blocks
    private static final double FAR_DISTANCE_SQ = 128.0 * 128.0;    // 128 blocks

    private EntityAIOptimizer() {}

    /**
     * Calculate the tick interval for an entity's AI based on distance from
     * the nearest player. Entities further away tick less frequently.
     *
     * Returns the number of ticks between full AI updates:
     * - Near (< 32 blocks): every tick (1)
     * - Medium (32-64 blocks): every 2 ticks
     * - Far (64-128 blocks): every 4 ticks
     * - Very far (> 128 blocks): every 8 ticks
     *
     * @param distanceSqToNearestPlayer squared distance to nearest player
     * @param currentTick               the current server tick
     * @param entityId                  entity's numeric ID for tick offset
     * @return true if the entity should tick its AI this tick
     */
    public static boolean shouldTickAI(double distanceSqToNearestPlayer, long currentTick, int entityId) {
        if (!SurvivalCoreConfig.get().entityAiDistanceBasedTickFrequency) {
            return true;
        }

        int interval;
        if (distanceSqToNearestPlayer < NEAR_DISTANCE_SQ) {
            interval = 1;
        } else if (distanceSqToNearestPlayer < MEDIUM_DISTANCE_SQ) {
            interval = 2;
        } else if (distanceSqToNearestPlayer < FAR_DISTANCE_SQ) {
            interval = 4;
        } else {
            interval = 8;
        }

        // Use entity ID as offset to distribute load across ticks
        // instead of all entities skipping the same ticks
        return (currentTick + entityId) % interval == 0;
    }

    /**
     * Whether to throttle the goal selector for an inactive entity.
     * Inactive entities only run goal selector every N ticks.
     *
     * @param isInactive  whether the entity is outside activation range
     * @param currentTick the current server tick
     * @param entityId    entity's numeric ID for tick offset
     * @return true if goal selector should be skipped this tick
     */
    public static boolean shouldThrottleGoalSelector(boolean isInactive, long currentTick, int entityId) {
        if (!SurvivalCoreConfig.get().entityAiGoalSelectorThrottle) {
            return false;
        }

        if (!isInactive) return false;

        // Inactive entities run goal selector every 20 ticks (1 second)
        return (currentTick + entityId) % 20 != 0;
    }

    /**
     * DAB: Dynamic Activation of Brain
     *
     * Continuous gradient from tick interval 1 (at startDistance) to maxInterval
     * (at activation range). Uses linear interpolation for smooth scaling.
     * Uses (currentTick + entityId) % interval == 0 for staggered distribution.
     *
     * More aggressive than fixed-tier system - entities gradually tick less
     * frequently as they move away from players, without discrete jumps.
     *
     * @param distanceSqToNearestPlayer squared distance to nearest player
     * @param currentTick               the current server tick
     * @param entityId                  entity's numeric ID for tick offset
     * @param activationRangeSq         squared activation range (from Paper config)
     * @return true if the entity should tick its AI this tick
     */
    public static boolean shouldTickAI_DAB(double distanceSqToNearestPlayer, long currentTick, int entityId, double activationRangeSq) {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        if (!config.dabEnabled) {
            return true;
        }

        double startDistSq = config.dabStartDistance * config.dabStartDistance;
        int maxInterval = config.dabMaxTickInterval;

        // Always tick when close
        if (distanceSqToNearestPlayer <= startDistSq) {
            return true;
        }

        // Maximum interval at activation range
        if (distanceSqToNearestPlayer >= activationRangeSq) {
            return (currentTick + entityId) % maxInterval == 0;
        }

        // Linear interpolation between startDistance and activationRange
        double t = (distanceSqToNearestPlayer - startDistSq) / (activationRangeSq - startDistSq);
        int interval = 1 + (int) (t * (maxInterval - 1));

        return (currentTick + entityId) % interval == 0;
    }

    /**
     * Calculate the batch group for brain tick batching.
     * Entities of the same type in the same batch group are ticked together
     * for better CPU cache utilization.
     *
     * @param entityTypeId numeric type identifier
     * @param batchSize    number of batch groups
     * @param currentTick  the current server tick
     * @return the batch group (0 to batchSize-1)
     */
    public static int getBrainTickBatch(int entityTypeId, int batchSize, long currentTick) {
        return (int) (currentTick % batchSize);
    }
}
