package net.survivalcore.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Configuration preset system.
 * Allows quick switching between optimization profiles.
 */
public final class PresetManager {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");

    public enum Preset {
        CONSERVATIVE("conservative", "Minimal optimizations, maximum compatibility"),
        BALANCED("balanced", "Recommended defaults for most servers"),
        AGGRESSIVE("aggressive", "Maximum performance, may affect vanilla mechanics slightly"),
        POTATO("potato", "Extreme optimization for 4-core / low-memory servers");

        public final String id;
        public final String description;

        Preset(String id, String description) {
            this.id = id;
            this.description = description;
        }
    }

    private PresetManager() {}

    /**
     * Apply a preset to the configuration file.
     * @param serverRoot server root path
     * @param preset the preset to apply
     * @return true if successfully applied
     */
    public static boolean applyPreset(Path serverRoot, Preset preset) {
        Path configPath = serverRoot.resolve("config").resolve("survivalcore.yml");
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

            CommentedConfigurationNode root = loader.load();

            switch (preset) {
                case CONSERVATIVE -> applyConservative(root);
                case BALANCED -> applyBalanced(root);
                case AGGRESSIVE -> applyAggressive(root);
                case POTATO -> applyPotato(root);
            }

            root.node("presets", "current").set(preset.id);
            loader.save(root);
            LOGGER.info("Applied preset: " + preset.id);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to apply preset " + preset.id, e);
            return false;
        }
    }

    private static void applyConservative(CommentedConfigurationNode root) throws org.spongepowered.configurate.serialize.SerializationException {
        // Disable most aggressive features, keep safe ones
        root.node("performance", "entity-ai", "dab", "enabled").set(false);
        root.node("performance", "entity-budgets", "enabled").set(false);
        root.node("performance", "tnt-batching", "enabled").set(false);
        root.node("performance", "farm-detection", "enabled").set(false);
        root.node("redstone", "observer-debounce", "enabled").set(false);
        root.node("survival", "entity-cleanup", "enabled").set(false);
        // Keep async systems, fast math, hopper optimization
    }

    private static void applyBalanced(CommentedConfigurationNode root) throws org.spongepowered.configurate.serialize.SerializationException {
        // Recommended defaults (the defaults in config)
        root.node("performance", "entity-ai", "dab", "enabled").set(true);
        root.node("performance", "entity-ai", "dab", "max-tick-interval").set(20);
        root.node("performance", "entity-budgets", "enabled").set(true);
        root.node("performance", "entity-budgets", "max-entity-time-ms").set(25);
        root.node("performance", "farm-detection", "soft-threshold").set(50);
        root.node("performance", "farm-detection", "hard-threshold").set(100);
        root.node("performance", "farm-detection", "critical-threshold").set(200);
        root.node("survival", "entity-cleanup", "soft-limit").set(3000);
        root.node("survival", "entity-cleanup", "hard-limit").set(5000);
    }

    private static void applyAggressive(CommentedConfigurationNode root) throws org.spongepowered.configurate.serialize.SerializationException {
        // More aggressive thresholds
        root.node("performance", "entity-ai", "dab", "enabled").set(true);
        root.node("performance", "entity-ai", "dab", "max-tick-interval").set(40);
        root.node("performance", "entity-budgets", "enabled").set(true);
        root.node("performance", "entity-budgets", "max-entity-time-ms").set(15);
        root.node("performance", "farm-detection", "soft-threshold").set(30);
        root.node("performance", "farm-detection", "hard-threshold").set(60);
        root.node("performance", "farm-detection", "critical-threshold").set(120);
        root.node("survival", "entity-cleanup", "soft-limit").set(2000);
        root.node("survival", "entity-cleanup", "hard-limit").set(3500);
        root.node("performance", "item-merge", "cooldown-ticks").set(3);
    }

    private static void applyPotato(CommentedConfigurationNode root) throws org.spongepowered.configurate.serialize.SerializationException {
        // Extreme optimization for weak hardware
        applyAggressive(root);
        root.node("performance", "entity-ai", "dab", "max-tick-interval").set(60);
        root.node("performance", "entity-budgets", "max-entity-time-ms").set(10);
        root.node("performance", "farm-detection", "soft-threshold").set(20);
        root.node("performance", "farm-detection", "hard-threshold").set(40);
        root.node("performance", "farm-detection", "critical-threshold").set(80);
        root.node("survival", "entity-cleanup", "soft-limit").set(1500);
        root.node("survival", "entity-cleanup", "hard-limit").set(2500);
        root.node("async", "entity-tracker", "max-threads").set(1);
        root.node("async", "pathfinding", "max-threads").set(1);
    }

    /**
     * Get preset by name string.
     */
    public static Preset fromString(String name) {
        for (Preset p : Preset.values()) {
            if (p.id.equalsIgnoreCase(name)) return p;
        }
        return null;
    }
}
