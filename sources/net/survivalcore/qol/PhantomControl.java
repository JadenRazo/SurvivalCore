package net.survivalcore.qol;

import java.util.logging.Logger;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Phantom spawning control.
 * Called from PhantomSpawner patch to control when phantoms can spawn.
 */
public final class PhantomControl {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");

    private PhantomControl() {}

    /**
     * Whether phantom spawning is globally enabled.
     */
    public static boolean isPhantomsEnabled() {
        return SurvivalCoreConfig.get().qolPhantomsEnabled;
    }

    /**
     * Check if a phantom should spawn for this player.
     *
     * @param insomniaTicks how long since player last slept
     * @param hasSkyAccess whether player has sky access at current position
     * @param playerY player Y coordinate
     * @param seaLevel world sea level
     * @return true if phantom should spawn
     */
    public static boolean shouldSpawnPhantom(long insomniaTicks, boolean hasSkyAccess, double playerY, int seaLevel) {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();

        if (!config.qolPhantomsEnabled) return false;

        if (insomniaTicks < config.qolPhantomsMinInsomniaTicks) return false;

        if (config.qolPhantomsRequireSky && !hasSkyAccess) return false;

        return true;
    }
}
