package net.survivalcore.qol;

import java.util.logging.Logger;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Per-mob-type griefing control.
 * Called from various entity and block patches to control mob world interaction.
 */
public final class MobGriefingControl {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");

    private MobGriefingControl() {}

    /**
     * Whether creeper explosions should destroy blocks.
     */
    public static boolean allowCreeperGriefing() {
        return SurvivalCoreConfig.get().qolMobGriefingCreeper;
    }

    /**
     * Whether enderman can pick up blocks.
     */
    public static boolean allowEndermanPickup() {
        return SurvivalCoreConfig.get().qolMobGriefingEnderman;
    }

    /**
     * Whether ghast fireballs should destroy blocks.
     */
    public static boolean allowGhastGriefing() {
        return SurvivalCoreConfig.get().qolMobGriefingGhast;
    }

    /**
     * Whether wither should destroy blocks.
     */
    public static boolean allowWitherGriefing() {
        return SurvivalCoreConfig.get().qolMobGriefingWither;
    }

    /**
     * Whether ravager should destroy blocks.
     */
    public static boolean allowRavagerGriefing() {
        return SurvivalCoreConfig.get().qolMobGriefingRavager;
    }

    /**
     * Whether entities trampling farmland should convert it to dirt.
     */
    public static boolean allowFarmlandTrampling() {
        return SurvivalCoreConfig.get().qolMobGriefingFarmlandTrampling;
    }

    /**
     * Check griefing permission by entity class name.
     * Convenience method for patch code that has entity class name available.
     *
     * @param entityClassName simple class name of the entity
     * @return true if the entity type is allowed to grief
     */
    public static boolean allowGriefingByType(String entityClassName) {
        return switch (entityClassName) {
            case "Creeper" -> allowCreeperGriefing();
            case "EnderMan" -> allowEndermanPickup();
            case "Ghast" -> allowGhastGriefing();
            case "WitherBoss" -> allowWitherGriefing();
            case "Ravager" -> allowRavagerGriefing();
            default -> true; // Unknown types use vanilla behavior
        };
    }
}
