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

    // ── Performance: DAB ──────────────────────────────────
    public final boolean dabEnabled;
    public final int dabStartDistance;
    public final int dabMaxTickInterval;

    // ── Performance: Entity Budgets ───────────────────────
    public final boolean entityBudgetsEnabled;
    public final int entityBudgetsMaxEntityTimeMs;

    // ── Performance: Item Merge ───────────────────────────
    public final boolean itemMergeThrottleEnabled;
    public final int itemMergeCooldownTicks;

    // ── Performance: Farm Detection ───────────────────────
    public final boolean farmDetectionEnabled;
    public final int farmSoftThreshold;
    public final int farmHardThreshold;
    public final int farmCriticalThreshold;
    public final boolean farmAlertAdmins;

    // ── Performance: TNT Batching ─────────────────────────
    public final boolean tntBatchingEnabled;
    public final double tntGroupRadius;

    // ── Performance: Collision ────────────────────────────
    public final boolean collisionSkipStationary;

    // ── Performance: Memory ──────────────────────────────
    public final boolean objectPoolingEnabled;

    // ── Performance: Random ──────────────────────────────
    public final boolean fasterRandomEnabled;
    public final String fasterRandomGenerator;

    // ── Redstone ─────────────────────────────────────────
    public final String redstoneImplementation;

    // ── Redstone: Observer Debounce ───────────────────────
    public final boolean observerDebounceEnabled;
    public final int observerDebounceMinIntervalTicks;

    // ── Redstone: Chunk Throttle ──────────────────────────
    public final boolean redstoneChunkThrottleEnabled;
    public final int redstoneChunkThrottleSoft;
    public final int redstoneChunkThrottleHard;
    public final int redstoneChunkThrottleCritical;
    public final boolean redstoneChunkThrottleAlertAdmins;

    // ── Survival: Entity Cleanup ──────────────────────────
    public final boolean entityCleanupEnabled;
    public final int entityCleanupSoftLimit;
    public final int entityCleanupHardLimit;

    // ── Fixes ─────────────────────────────────────────────
    public final boolean fixEnderPearlBorderBypass;
    public final boolean fixChorusFruitWallPhase;
    public final boolean fixHeadlessPiston;
    public final boolean fixBedExplosions;
    public final boolean fixPortalTrapProtection;
    public final int fixTntDupeLimit;

    // ── QoL: Phantoms ─────────────────────────────────────
    public final boolean qolPhantomsEnabled;
    public final int qolPhantomsMinInsomniaTicks;
    public final boolean qolPhantomsRequireSky;

    // ── QoL: Sleep ────────────────────────────────────────
    public final boolean qolSinglePlayerSleep;
    public final boolean qolSleepClearWeather;
    public final boolean qolSleepResetPhantoms;

    // ── QoL: Player Head Drops ────────────────────────────
    public final boolean qolPlayerHeadDropsEnabled;
    public final double qolPlayerHeadDropsChance;

    // ── QoL: Mob Griefing ─────────────────────────────────
    public final boolean qolMobGriefingCreeper;
    public final boolean qolMobGriefingEnderman;
    public final boolean qolMobGriefingGhast;
    public final boolean qolMobGriefingWither;
    public final boolean qolMobGriefingRavager;
    public final boolean qolMobGriefingFarmlandTrampling;

    // ── QoL: Items & Experience ───────────────────────────
    public final int qolItemDespawnTimeTicks;
    public final double qolExpMultiplier;

    // ── QoL: Knockback ────────────────────────────────────
    public final double qolKnockbackPlayerMultiplier;
    public final double qolKnockbackMobMultiplier;

    // ── Admin ─────────────────────────────────────────────
    public final int adminMobSpawningMaxPerChunk;
    public final boolean adminAutoCleanupEnabled;
    public final int adminAutoCleanupMaxItemsPerChunk;
    public final int adminAutoCleanupMaxMinecartsPerChunk;
    public final int adminAutoCleanupIntervalTicks;
    public final boolean adminLogExplosions;
    public final boolean adminLogFireSpread;
    public final boolean adminLogEntityDamage;

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

        // DAB
        CommentedConfigurationNode dab = ai.node("dab");
        this.dabEnabled = dab.node("enabled").getBoolean(true);
        this.dabStartDistance = dab.node("start-distance").getInt(12);
        this.dabMaxTickInterval = dab.node("max-tick-interval").getInt(20);

        // Entity budgets
        CommentedConfigurationNode budgets = perf.node("entity-budgets");
        this.entityBudgetsEnabled = budgets.node("enabled").getBoolean(true);
        this.entityBudgetsMaxEntityTimeMs = budgets.node("max-entity-time-ms").getInt(25);

        // Item merge
        CommentedConfigurationNode itemMerge = perf.node("item-merge");
        this.itemMergeThrottleEnabled = itemMerge.node("throttle-enabled").getBoolean(true);
        this.itemMergeCooldownTicks = itemMerge.node("cooldown-ticks").getInt(5);

        // Farm detection
        CommentedConfigurationNode farm = perf.node("farm-detection");
        this.farmDetectionEnabled = farm.node("enabled").getBoolean(true);
        this.farmSoftThreshold = farm.node("soft-threshold").getInt(50);
        this.farmHardThreshold = farm.node("hard-threshold").getInt(100);
        this.farmCriticalThreshold = farm.node("critical-threshold").getInt(200);
        this.farmAlertAdmins = farm.node("alert-admins").getBoolean(true);

        // TNT batching
        CommentedConfigurationNode tnt = perf.node("tnt-batching");
        this.tntBatchingEnabled = tnt.node("enabled").getBoolean(true);
        this.tntGroupRadius = tnt.node("group-radius").getDouble(1.0);

        // Collision
        this.collisionSkipStationary = perf.node("collision", "skip-stationary").getBoolean(true);

        this.objectPoolingEnabled = perf.node("memory", "object-pooling").getBoolean(true);

        CommentedConfigurationNode random = perf.node("faster-random");
        this.fasterRandomEnabled = random.node("enabled").getBoolean(true);
        this.fasterRandomGenerator = random.node("generator").getString("Xoroshiro128PlusPlus");

        this.redstoneImplementation = root.node("redstone", "implementation").getString("alternate-current");

        // Observer debounce
        CommentedConfigurationNode observer = root.node("redstone", "observer-debounce");
        this.observerDebounceEnabled = observer.node("enabled").getBoolean(true);
        this.observerDebounceMinIntervalTicks = observer.node("min-interval-ticks").getInt(4);

        // Chunk throttle
        CommentedConfigurationNode chunkThrottle = root.node("redstone", "chunk-throttle");
        this.redstoneChunkThrottleEnabled = chunkThrottle.node("enabled").getBoolean(true);
        this.redstoneChunkThrottleSoft = chunkThrottle.node("soft-threshold").getInt(64);
        this.redstoneChunkThrottleHard = chunkThrottle.node("hard-threshold").getInt(150);
        this.redstoneChunkThrottleCritical = chunkThrottle.node("critical-threshold").getInt(300);
        this.redstoneChunkThrottleAlertAdmins = chunkThrottle.node("alert-admins").getBoolean(true);

        // Entity cleanup
        CommentedConfigurationNode cleanup = root.node("survival", "entity-cleanup");
        this.entityCleanupEnabled = cleanup.node("enabled").getBoolean(true);
        this.entityCleanupSoftLimit = cleanup.node("soft-limit").getInt(3000);
        this.entityCleanupHardLimit = cleanup.node("hard-limit").getInt(5000);

        // Fixes
        CommentedConfigurationNode fixes = root.node("fixes");
        this.fixEnderPearlBorderBypass = fixes.node("ender-pearl-border-bypass").getBoolean(true);
        this.fixChorusFruitWallPhase = fixes.node("chorus-fruit-wall-phase").getBoolean(true);
        this.fixHeadlessPiston = fixes.node("headless-piston").getBoolean(true);
        this.fixBedExplosions = fixes.node("bed-explosions").getBoolean(true);
        this.fixPortalTrapProtection = fixes.node("portal-trap-protection").getBoolean(true);
        this.fixTntDupeLimit = fixes.node("tnt-dupe-limit").getInt(10);

        // QoL: Phantoms
        CommentedConfigurationNode phantoms = root.node("qol", "phantoms");
        this.qolPhantomsEnabled = phantoms.node("enabled").getBoolean(false);
        this.qolPhantomsMinInsomniaTicks = phantoms.node("min-insomnia-ticks").getInt(72000);
        this.qolPhantomsRequireSky = phantoms.node("require-sky").getBoolean(true);

        // QoL: Sleep
        CommentedConfigurationNode sleep = root.node("qol", "sleep");
        this.qolSinglePlayerSleep = sleep.node("single-player-sleep").getBoolean(true);
        this.qolSleepClearWeather = sleep.node("clear-weather").getBoolean(true);
        this.qolSleepResetPhantoms = sleep.node("reset-phantoms").getBoolean(true);

        // QoL: Player head drops
        CommentedConfigurationNode heads = root.node("qol", "player-head-drops");
        this.qolPlayerHeadDropsEnabled = heads.node("enabled").getBoolean(true);
        this.qolPlayerHeadDropsChance = heads.node("chance").getDouble(1.0);

        // QoL: Mob griefing
        CommentedConfigurationNode grief = root.node("qol", "mob-griefing");
        this.qolMobGriefingCreeper = grief.node("creeper").getBoolean(true);
        this.qolMobGriefingEnderman = grief.node("enderman").getBoolean(true);
        this.qolMobGriefingGhast = grief.node("ghast").getBoolean(true);
        this.qolMobGriefingWither = grief.node("wither").getBoolean(true);
        this.qolMobGriefingRavager = grief.node("ravager").getBoolean(true);
        this.qolMobGriefingFarmlandTrampling = grief.node("farmland-trampling").getBoolean(true);

        // QoL: Items & experience
        this.qolItemDespawnTimeTicks = root.node("qol", "item-despawn-time-ticks").getInt(6000);
        this.qolExpMultiplier = root.node("qol", "exp-multiplier").getDouble(1.0);

        // QoL: Knockback
        CommentedConfigurationNode knockback = root.node("qol", "knockback");
        this.qolKnockbackPlayerMultiplier = knockback.node("player-multiplier").getDouble(1.0);
        this.qolKnockbackMobMultiplier = knockback.node("mob-multiplier").getDouble(1.0);

        // Admin
        CommentedConfigurationNode admin = root.node("admin");
        this.adminMobSpawningMaxPerChunk = admin.node("mob-spawning", "max-per-chunk").getInt(50);
        CommentedConfigurationNode autoCleanup = admin.node("auto-cleanup");
        this.adminAutoCleanupEnabled = autoCleanup.node("enabled").getBoolean(true);
        this.adminAutoCleanupMaxItemsPerChunk = autoCleanup.node("max-items-per-chunk").getInt(500);
        this.adminAutoCleanupMaxMinecartsPerChunk = autoCleanup.node("max-minecarts-per-chunk").getInt(50);
        this.adminAutoCleanupIntervalTicks = autoCleanup.node("interval-ticks").getInt(1200);

        CommentedConfigurationNode logging = admin.node("logging");
        this.adminLogExplosions = logging.node("explosions").getBoolean(true);
        this.adminLogFireSpread = logging.node("fire-spread").getBoolean(false);
        this.adminLogEntityDamage = logging.node("entity-damage").getBoolean(false);

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

        // DAB
        CommentedConfigurationNode dab = ai.node("dab");
        dab.comment("Distance-Activation-Behavior: reduce tick rate for distant entities.");
        setDefault(dab.node("enabled"), true, null);
        setDefault(dab.node("start-distance"), 12, "Distance in chunks to start reducing tick rate");
        setDefault(dab.node("max-tick-interval"), 20, "Max ticks between entity updates");

        // Entity budgets
        CommentedConfigurationNode budgets = perf.node("entity-budgets");
        budgets.comment("Per-tick entity processing budget to prevent lag spikes.");
        setDefault(budgets.node("enabled"), true, null);
        setDefault(budgets.node("max-entity-time-ms"), 25, "Max milliseconds per tick for entity processing");

        // Item merge
        CommentedConfigurationNode itemMerge = perf.node("item-merge");
        itemMerge.comment("Throttle item merging to reduce item entity overhead.");
        setDefault(itemMerge.node("throttle-enabled"), true, null);
        setDefault(itemMerge.node("cooldown-ticks"), 5, "Ticks between merge checks");

        // Farm detection
        CommentedConfigurationNode farm = perf.node("farm-detection");
        farm.comment("Detect and throttle entity farm hotspots.");
        setDefault(farm.node("enabled"), true, null);
        setDefault(farm.node("soft-threshold"), 50, "Entity count to start throttling");
        setDefault(farm.node("hard-threshold"), 100, "Heavy throttling threshold");
        setDefault(farm.node("critical-threshold"), 200, "Emergency throttling threshold");
        setDefault(farm.node("alert-admins"), true, "Alert ops when critical threshold reached");

        // TNT batching
        CommentedConfigurationNode tnt = perf.node("tnt-batching");
        tnt.comment("Batch nearby TNT explosions to reduce calculation overhead.");
        setDefault(tnt.node("enabled"), true, null);
        setDefault(tnt.node("group-radius"), 1.0, "TNT within this radius explodes together");

        // Collision
        setDefault(perf.node("collision", "skip-stationary"), true, "Skip collision checks for stationary entities");

        setDefault(perf.node("memory", "object-pooling"), true, "Thread-local object pools for BlockPos, Vec3, AABB.");

        setDefault(perf.node("faster-random", "enabled"), true, "Use faster random number generators.");
        setDefault(perf.node("faster-random", "generator"), "Xoroshiro128PlusPlus", null);

        // Redstone
        CommentedConfigurationNode redstone = root.node("redstone");
        redstone.comment("Redstone engine configuration.");
        setDefault(redstone.node("implementation"), "alternate-current", "Options: alternate-current, vanilla, eigencraft");

        // Observer debounce
        CommentedConfigurationNode observer = redstone.node("observer-debounce");
        observer.comment("Debounce rapid observer updates to prevent lag machines.");
        setDefault(observer.node("enabled"), true, null);
        setDefault(observer.node("min-interval-ticks"), 4, "Minimum ticks between observer activations");

        // Chunk throttle
        CommentedConfigurationNode chunkThrottle = redstone.node("chunk-throttle");
        chunkThrottle.comment("Throttle redstone updates per chunk based on update density.\nPrevents lag machines from tanking server TPS.");
        setDefault(chunkThrottle.node("enabled"), true, null);
        setDefault(chunkThrottle.node("soft-threshold"), 64, "Updates/tick to start throttling (divisor 2)");
        setDefault(chunkThrottle.node("hard-threshold"), 150, "Heavy throttling threshold (divisor 4)");
        setDefault(chunkThrottle.node("critical-threshold"), 300, "Emergency throttling threshold (divisor 8)");
        setDefault(chunkThrottle.node("alert-admins"), true, "Log warnings when chunks hit hard/critical thresholds");

        // Survival: Entity cleanup
        CommentedConfigurationNode survival = root.node("survival");
        CommentedConfigurationNode cleanup = survival.node("entity-cleanup");
        cleanup.comment("Automatic entity count limiting to prevent server overload.");
        setDefault(cleanup.node("enabled"), true, null);
        setDefault(cleanup.node("soft-limit"), 3000, "Start cleanup warnings");
        setDefault(cleanup.node("hard-limit"), 5000, "Force entity removal");

        // Fixes
        CommentedConfigurationNode fixes = root.node("fixes");
        fixes.comment("Exploit and bug fixes.");
        setDefault(fixes.node("ender-pearl-border-bypass"), true, "Prevent ender pearls from teleporting through world borders");
        setDefault(fixes.node("chorus-fruit-wall-phase"), true, "Prevent chorus fruit from phasing through walls");
        setDefault(fixes.node("headless-piston"), true, "Fix headless piston duplication");
        setDefault(fixes.node("bed-explosions"), true, "Fix bed explosion exploit");
        setDefault(fixes.node("portal-trap-protection"), true, "Prevent portal trapping exploits");
        setDefault(fixes.node("tnt-dupe-limit"), 10, "Max TNT duplication per tick (0 = disable duping entirely)");

        // QoL: Phantoms
        CommentedConfigurationNode qol = root.node("qol");
        qol.comment("Quality of life improvements.");
        CommentedConfigurationNode phantoms = qol.node("phantoms");
        phantoms.comment("Phantom spawning adjustments.");
        setDefault(phantoms.node("enabled"), false, "Enable custom phantom behavior (false = vanilla)");
        setDefault(phantoms.node("min-insomnia-ticks"), 72000, "Ticks before phantoms spawn (vanilla: 72000 = 1 hour)");
        setDefault(phantoms.node("require-sky"), true, "Only spawn if player can see sky");

        // QoL: Sleep
        CommentedConfigurationNode sleep = qol.node("sleep");
        sleep.comment("Sleep mechanics.");
        setDefault(sleep.node("single-player-sleep"), true, "One player sleeping skips night");
        setDefault(sleep.node("clear-weather"), true, "Sleeping clears weather");
        setDefault(sleep.node("reset-phantoms"), true, "Sleeping resets phantom timer");

        // QoL: Player head drops
        CommentedConfigurationNode heads = qol.node("player-head-drops");
        heads.comment("Player head drops on PvP death.");
        setDefault(heads.node("enabled"), true, null);
        setDefault(heads.node("chance"), 1.0, "Drop chance (1.0 = always, 0.0 = never)");

        // QoL: Mob griefing
        CommentedConfigurationNode grief = qol.node("mob-griefing");
        grief.comment("Fine-grained mob griefing control (per mob type).");
        setDefault(grief.node("creeper"), true, "Creepers destroy blocks");
        setDefault(grief.node("enderman"), true, "Endermen pick up blocks");
        setDefault(grief.node("ghast"), true, "Ghasts destroy blocks");
        setDefault(grief.node("wither"), true, "Withers destroy blocks");
        setDefault(grief.node("ravager"), true, "Ravagers destroy crops");
        setDefault(grief.node("farmland-trampling"), true, "Mobs trample farmland");

        // QoL: Items & experience
        setDefault(qol.node("item-despawn-time-ticks"), 6000, "Item despawn time (vanilla: 6000 = 5 minutes)");
        setDefault(qol.node("exp-multiplier"), 1.0, "Experience orb multiplier");

        // QoL: Knockback
        CommentedConfigurationNode knockback = qol.node("knockback");
        knockback.comment("Knockback multipliers.");
        setDefault(knockback.node("player-multiplier"), 1.0, "Knockback received by players");
        setDefault(knockback.node("mob-multiplier"), 1.0, "Knockback received by mobs");

        // Admin
        CommentedConfigurationNode admin = root.node("admin");
        admin.comment("Administrative tools and limits.");
        setDefault(admin.node("mob-spawning", "max-per-chunk"), 50, "Max mobs per chunk");

        CommentedConfigurationNode autoCleanup = admin.node("auto-cleanup");
        autoCleanup.comment("Automatic cleanup of excessive entities.");
        setDefault(autoCleanup.node("enabled"), true, null);
        setDefault(autoCleanup.node("max-items-per-chunk"), 500, "Max item entities per chunk");
        setDefault(autoCleanup.node("max-minecarts-per-chunk"), 50, "Max minecarts per chunk");
        setDefault(autoCleanup.node("interval-ticks"), 1200, "Cleanup check interval (1200 = 1 minute)");

        CommentedConfigurationNode logging = admin.node("logging");
        logging.comment("Action logging for sensitive events.");
        setDefault(logging.node("explosions"), true, "Log all explosions");
        setDefault(logging.node("fire-spread"), false, "Log fire spread events");
        setDefault(logging.node("entity-damage"), false, "Log entity block damage");

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
