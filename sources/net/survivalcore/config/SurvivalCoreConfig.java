package net.survivalcore.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Global configuration for SurvivalCore optimizations.
 * Loaded from config/survivalcore.yml in the server root.
 *
 * Uses Configurate (already bundled with Paper) for YAML parsing
 * with comment support.
 */
public final class SurvivalCoreConfig {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static SurvivalCoreConfig instance;

    // ── Async: Entity Tracker ────────────────────────────
    public final boolean asyncEntityTrackerEnabled;
    public final int asyncEntityTrackerMaxThreads;
    public final boolean asyncEntityTrackerCompatMode;

    // ── Async: Pathfinding ───────────────────────────────
    public final boolean asyncPathfindingEnabled;
    public final int asyncPathfindingMaxThreads;

    // ── Async: Mob Spawning ──────────────────────────────
    public final boolean asyncMobSpawningEnabled;

    // ── Performance: SIMD ────────────────────────────────
    public final boolean simdEnabled;

    // ── Performance: FastMath ─────────────────────────────
    public final boolean fastMathEnabled;

    // ── Performance: Hopper ──────────────────────────────
    public final boolean hopperOptimizedCaching;
    public final boolean hopperSkipEmptyCheck;
    public final boolean hopperThrottleWhenFull;

    // ── Performance: Entity AI ───────────────────────────
    public final boolean entityAiGoalSelectorThrottle;
    public final boolean entityAiDistanceBasedTickFrequency;
    public final boolean entityAiBrainTickBatching;

    // ── Performance: Memory ──────────────────────────────
    public final boolean objectPoolingEnabled;

    // ── Performance: Random ──────────────────────────────
    public final boolean fasterRandomEnabled;
    public final String fasterRandomGenerator;

    // ── Redstone ─────────────────────────────────────────
    public final String redstoneImplementation;

    // ── Monitoring ───────────────────────────────────────
    public final boolean monitoringEnabled;
    public final int monitoringReportInterval;

    // ── Compatibility ────────────────────────────────────
    public final boolean villagerLobotomizeEnabled;
    public final int villagerLobotomizeCheckInterval;

    private SurvivalCoreConfig(CommentedConfigurationNode root) {
        CommentedConfigurationNode async = root.node("async");
        CommentedConfigurationNode tracker = async.node("entity-tracker");
        this.asyncEntityTrackerEnabled = tracker.node("enabled").getBoolean(true);
        this.asyncEntityTrackerMaxThreads = tracker.node("max-threads").getInt(0);
        this.asyncEntityTrackerCompatMode = tracker.node("compat-mode").getBoolean(true);

        CommentedConfigurationNode pathfinding = async.node("pathfinding");
        this.asyncPathfindingEnabled = pathfinding.node("enabled").getBoolean(true);
        this.asyncPathfindingMaxThreads = pathfinding.node("max-threads").getInt(0);

        this.asyncMobSpawningEnabled = async.node("mob-spawning", "enabled").getBoolean(true);

        CommentedConfigurationNode perf = root.node("performance");
        this.simdEnabled = perf.node("simd", "enabled").getBoolean(true);
        this.fastMathEnabled = perf.node("fast-math", "enabled").getBoolean(true);

        CommentedConfigurationNode hopper = perf.node("hopper");
        this.hopperOptimizedCaching = hopper.node("optimized-inventory-caching").getBoolean(true);
        this.hopperSkipEmptyCheck = hopper.node("skip-empty-check").getBoolean(true);
        this.hopperThrottleWhenFull = hopper.node("throttle-when-full").getBoolean(true);

        CommentedConfigurationNode ai = perf.node("entity-ai");
        this.entityAiGoalSelectorThrottle = ai.node("inactive-goal-selector-throttle").getBoolean(true);
        this.entityAiDistanceBasedTickFrequency = ai.node("distance-based-tick-frequency").getBoolean(true);
        this.entityAiBrainTickBatching = ai.node("brain-tick-batching").getBoolean(true);

        this.objectPoolingEnabled = perf.node("memory", "object-pooling").getBoolean(true);

        CommentedConfigurationNode random = perf.node("faster-random");
        this.fasterRandomEnabled = random.node("enabled").getBoolean(true);
        this.fasterRandomGenerator = random.node("generator").getString("Xoroshiro128PlusPlus");

        this.redstoneImplementation = root.node("redstone", "implementation").getString("alternate-current");

        CommentedConfigurationNode monitoring = root.node("monitoring");
        this.monitoringEnabled = monitoring.node("enabled").getBoolean(true);
        this.monitoringReportInterval = monitoring.node("report-interval").getInt(6000);

        CommentedConfigurationNode compat = root.node("compatibility");
        CommentedConfigurationNode lobotomize = compat.node("villager-lobotomize");
        this.villagerLobotomizeEnabled = lobotomize.node("enabled").getBoolean(true);
        this.villagerLobotomizeCheckInterval = lobotomize.node("check-interval").getInt(100);
    }

    public static SurvivalCoreConfig get() {
        if (instance == null) {
            throw new IllegalStateException("SurvivalCoreConfig not initialized");
        }
        return instance;
    }

    public static void init(Path serverRoot) {
        Path configDir = serverRoot.resolve("config");
        Path configPath = configDir.resolve("survivalcore.yml");

        try {
            Files.createDirectories(configDir);

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

            CommentedConfigurationNode root;
            if (Files.exists(configPath)) {
                root = loader.load();
            } else {
                root = loader.createNode();
                LOGGER.info("Creating default survivalcore.yml configuration...");
            }

            writeDefaults(root);
            loader.save(root);

            instance = new SurvivalCoreConfig(root);
            LOGGER.info("SurvivalCore configuration loaded");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load survivalcore.yml", e);
            throw new RuntimeException("Failed to initialize SurvivalCore configuration", e);
        }
    }

    public int resolveEntityTrackerThreads() {
        if (asyncEntityTrackerMaxThreads > 0) return asyncEntityTrackerMaxThreads;
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
    }

    public int resolvePathfindingThreads() {
        if (asyncPathfindingMaxThreads > 0) return asyncPathfindingMaxThreads;
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 3);
    }

    private static void writeDefaults(CommentedConfigurationNode root) {
        // Async section
        CommentedConfigurationNode async = root.node("async");
        async.comment("Async threading optimizations. Thread count 0 = auto-detect.\nRestart required for thread count changes.");

        setDefault(async.node("entity-tracker", "enabled"), true, "Move entity position/velocity/metadata broadcasts off the main thread.");
        setDefault(async.node("entity-tracker", "max-threads"), 0, "0 = auto (cores/4, minimum 1)");
        setDefault(async.node("entity-tracker", "compat-mode"), true, "Sync tracking for NPC-type entities (Citizens, FancyNpcs).");

        setDefault(async.node("pathfinding", "enabled"), true, "Submit A* pathfinding calculations to a worker thread pool.");
        setDefault(async.node("pathfinding", "max-threads"), 0, "0 = auto (cores/3, minimum 1)");

        setDefault(async.node("mob-spawning", "enabled"), true, "Calculate spawn positions async; entity creation stays on main thread.");

        // Performance section
        CommentedConfigurationNode perf = root.node("performance");
        perf.comment("Computation and memory optimizations.");

        setDefault(perf.node("simd", "enabled"), true, "Use Java Vector API (SIMD) for data-parallel operations.\nRequires JVM flag: --add-modules=jdk.incubator.vector");
        setDefault(perf.node("fast-math", "enabled"), true, "Trig lookup tables, bit-manipulation floor/ceil, fast inverse sqrt.");

        CommentedConfigurationNode hopper = perf.node("hopper");
        hopper.comment("Lithium-style hopper optimizations.");
        setDefault(hopper.node("optimized-inventory-caching"), true, null);
        setDefault(hopper.node("skip-empty-check"), true, null);
        setDefault(hopper.node("throttle-when-full"), true, null);

        CommentedConfigurationNode ai = perf.node("entity-ai");
        ai.comment("Entity AI performance improvements.");
        setDefault(ai.node("inactive-goal-selector-throttle"), true, null);
        setDefault(ai.node("distance-based-tick-frequency"), true, null);
        setDefault(ai.node("brain-tick-batching"), true, null);

        setDefault(perf.node("memory", "object-pooling"), true, "Thread-local object pools for BlockPos, Vec3, AABB.");

        setDefault(perf.node("faster-random", "enabled"), true, "Use faster random number generators.");
        setDefault(perf.node("faster-random", "generator"), "Xoroshiro128PlusPlus", null);

        // Redstone
        CommentedConfigurationNode redstone = root.node("redstone");
        redstone.comment("Redstone engine configuration.");
        setDefault(redstone.node("implementation"), "alternate-current", "Options: alternate-current, vanilla, eigencraft");

        // Monitoring
        CommentedConfigurationNode monitoring = root.node("monitoring");
        monitoring.comment("Performance monitoring and reporting.");
        setDefault(monitoring.node("enabled"), true, null);
        setDefault(monitoring.node("report-interval"), 6000, "Ticks between performance reports (0 = disabled). Default 6000 = 5 minutes.");

        // Compatibility
        CommentedConfigurationNode compat = root.node("compatibility");
        CommentedConfigurationNode lobotomize = compat.node("villager-lobotomize");
        lobotomize.comment("Disable AI for villagers that can't pathfind to their job site.");
        setDefault(lobotomize.node("enabled"), true, null);
        setDefault(lobotomize.node("check-interval"), 100, null);
    }

    private static void setDefault(CommentedConfigurationNode node, Object value, String comment) {
        try {
            if (node.virtual()) {
                node.set(value);
            }
            if (comment != null && (node.comment() == null || node.comment().isEmpty())) {
                node.comment(comment);
            }
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            LOGGER.warning("Failed to set default for " + node.path() + ": " + e.getMessage());
        }
    }
}
