package net.survivalcore.async;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Lithium-style hopper optimizations.
 *
 * Key improvements over vanilla:
 * 1. Inventory caching: Weak refs to source/destination containers
 *    to avoid repeated block entity lookups per transfer attempt.
 * 2. Skip-empty optimization: Skip transfer checks until a
 *    ContainerChangeEvent signals new items are available.
 * 3. Entity search caching: Entities notify hoppers of their presence
 *    instead of hoppers polling for entities every tick.
 * 4. Batch transfers: Move multiple items in a single operation
 *    when transferring matching stacks.
 *
 * Each optimization can be independently toggled via config.
 */
public final class HopperOptimizer {

    /**
     * Tracks whether a hopper's source container has items worth checking.
     * Key: BlockPos packed as long (BlockPos.asLong())
     * Value: true if the source may have items, false if confirmed empty
     */
    private static final Map<Long, Boolean> sourceHasItems = new ConcurrentHashMap<>();

    /**
     * Tracks whether a hopper's destination container has room.
     * Key: BlockPos packed as long
     * Value: true if destination may have room, false if confirmed full
     */
    private static final Map<Long, Boolean> destHasRoom = new ConcurrentHashMap<>();

    /**
     * Weak references to cached container block entities.
     * Avoids repeated world.getBlockEntity() calls.
     */
    private static final Map<Long, WeakReference<Object>> containerCache = new ConcurrentHashMap<>();

    private HopperOptimizer() {}

    /**
     * Signal that a container's contents changed.
     * Called from ContainerChangeEvent or direct inventory modification.
     *
     * @param blockPosLong the packed block position
     */
    public static void onContainerChanged(long blockPosLong) {
        sourceHasItems.put(blockPosLong, true);
        destHasRoom.put(blockPosLong, true);
    }

    /**
     * Check if a hopper should skip its pull check because the source is empty.
     *
     * @param sourcePosLong packed position of the source container
     * @return true if the check should be skipped (source is empty)
     */
    public static boolean shouldSkipPull(long sourcePosLong) {
        if (!SurvivalCoreConfig.get().hopperSkipEmptyCheck) return false;
        Boolean hasItems = sourceHasItems.get(sourcePosLong);
        return hasItems != null && !hasItems;
    }

    /**
     * Mark a source container as empty after a failed pull attempt.
     */
    public static void markSourceEmpty(long sourcePosLong) {
        if (SurvivalCoreConfig.get().hopperSkipEmptyCheck) {
            sourceHasItems.put(sourcePosLong, false);
        }
    }

    /**
     * Check if a hopper should skip its push check because the dest is full.
     *
     * @param destPosLong packed position of the destination container
     * @return true if the check should be skipped (destination is full)
     */
    public static boolean shouldSkipPush(long destPosLong) {
        if (!SurvivalCoreConfig.get().hopperThrottleWhenFull) return false;
        Boolean hasRoom = destHasRoom.get(destPosLong);
        return hasRoom != null && !hasRoom;
    }

    /**
     * Mark a destination container as full after a failed push attempt.
     */
    public static void markDestFull(long destPosLong) {
        if (SurvivalCoreConfig.get().hopperThrottleWhenFull) {
            destHasRoom.put(destPosLong, false);
        }
    }

    /**
     * Cache a container block entity reference.
     */
    public static void cacheContainer(long posLong, Object container) {
        if (SurvivalCoreConfig.get().hopperOptimizedCaching) {
            containerCache.put(posLong, new WeakReference<>(container));
        }
    }

    /**
     * Retrieve a cached container, or null if not cached or GC'd.
     */
    public static Object getCachedContainer(long posLong) {
        if (!SurvivalCoreConfig.get().hopperOptimizedCaching) return null;
        WeakReference<Object> ref = containerCache.get(posLong);
        if (ref == null) return null;
        Object container = ref.get();
        if (container == null) {
            containerCache.remove(posLong);
        }
        return container;
    }

    /**
     * Remove a container from cache when the block is removed.
     */
    public static void removeContainer(long posLong) {
        containerCache.remove(posLong);
        sourceHasItems.remove(posLong);
        destHasRoom.remove(posLong);
    }

    /**
     * Clear all caches. Called during world unload or server shutdown.
     */
    public static void clearAll() {
        sourceHasItems.clear();
        destHasRoom.clear();
        containerCache.clear();
    }
}
