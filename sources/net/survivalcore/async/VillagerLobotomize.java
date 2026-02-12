package net.survivalcore.async;

import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Villager lobotomize optimization (re-implemented from Purpur).
 *
 * Disables AI for villagers that cannot pathfind to their job site or
 * points of interest. This prevents them from consuming excessive CPU
 * by constantly recalculating impossible paths.
 *
 * Lobotomized villagers still:
 * - Trade with players normally
 * - Display their profession
 * - Breed (if they can pathfind to beds)
 *
 * They just stop trying to path to unreachable locations.
 */
public final class VillagerLobotomize {

    private VillagerLobotomize() {}

    /**
     * Check if a villager should be lobotomized based on the current tick
     * and check interval. Uses the entity ID for offset distribution.
     *
     * @param currentTick server tick
     * @param entityId    villager entity ID
     * @return true if the lobotomize check should run this tick
     */
    public static boolean shouldCheck(long currentTick, int entityId) {
        if (!SurvivalCoreConfig.get().villagerLobotomizeEnabled) return false;
        int interval = SurvivalCoreConfig.get().villagerLobotomizeCheckInterval;
        return (currentTick + entityId) % interval == 0;
    }

    /**
     * Determine if a villager should be lobotomized based on pathfinding status.
     *
     * @param hasReachableJobSite whether the villager can reach its job site
     * @param hasReachableBed     whether the villager can reach a bed
     * @param isTrading           whether the villager is currently trading
     * @return true if the villager should have its AI disabled
     */
    public static boolean shouldLobotomize(boolean hasReachableJobSite, boolean hasReachableBed, boolean isTrading) {
        // Never lobotomize during active trading
        if (isTrading) return false;
        // Lobotomize if can't reach either job site or bed
        return !hasReachableJobSite && !hasReachableBed;
    }
}
