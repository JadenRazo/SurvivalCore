package net.survivalcore;

import java.nio.file.Path;
import java.util.logging.Logger;
import net.survivalcore.async.AsyncEntityTracker;
import net.survivalcore.async.AsyncMobSpawner;
import net.survivalcore.async.AsyncPathfinder;
import net.survivalcore.config.SurvivalCoreConfig;
import net.survivalcore.monitoring.PerformanceMonitor;

/**
 * SurvivalCore bootstrap and lifecycle manager.
 *
 * Initializes all optimization subsystems in the correct order during
 * server startup, and shuts them down cleanly on server stop.
 *
 * Called from MinecraftServer during bootstrap, before worlds load.
 */
public final class SurvivalCore {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    public static final String NAME = "SurvivalCore";
    public static final String VERSION = "1.0.0";
    private static boolean initialized = false;

    private SurvivalCore() {}

    /**
     * Initialize all SurvivalCore systems.
     * Called during MinecraftServer startup, before world loading.
     *
     * @param serverRoot the server root directory (for config file location)
     */
    public static void init(Path serverRoot) {
        if (initialized) return;

        LOGGER.info("Starting " + NAME + " v" + VERSION);
        LOGGER.info("Custom performance fork - https://gitlab.com/JadenRazo/SurvivalCore");

        // 1. Load configuration first (everything else depends on it)
        SurvivalCoreConfig.init(serverRoot);

        // 2. Initialize async thread pools
        AsyncEntityTracker.init();
        AsyncPathfinder.init();
        AsyncMobSpawner.init();

        // 3. Initialize monitoring
        PerformanceMonitor.init();

        // 4. Log SIMD status
        if (SurvivalCoreConfig.get().simdEnabled) {
            try {
                Class.forName("jdk.incubator.vector.FloatVector");
                LOGGER.info("SIMD Vector API: available");
            } catch (ClassNotFoundException e) {
                LOGGER.info("SIMD Vector API: not available (add --add-modules=jdk.incubator.vector)");
            }
        }

        // 5. Log configuration summary
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        LOGGER.info("Async entity tracking: " + (config.asyncEntityTrackerEnabled ? "ON (" + config.resolveEntityTrackerThreads() + " threads)" : "OFF"));
        LOGGER.info("Async pathfinding: " + (config.asyncPathfindingEnabled ? "ON (" + config.resolvePathfindingThreads() + " threads)" : "OFF"));
        LOGGER.info("Async mob spawning: " + (config.asyncMobSpawningEnabled ? "ON" : "OFF"));
        LOGGER.info("FastMath: " + (config.fastMathEnabled ? "ON" : "OFF"));
        LOGGER.info("Hopper optimization: " + (config.hopperOptimizedCaching ? "ON" : "OFF"));
        LOGGER.info("Entity AI throttling: " + (config.entityAiGoalSelectorThrottle ? "ON" : "OFF"));
        LOGGER.info("Object pooling: " + (config.objectPoolingEnabled ? "ON" : "OFF"));
        LOGGER.info("Redstone: " + config.redstoneImplementation);

        int cores = Runtime.getRuntime().availableProcessors();
        LOGGER.info("Available CPU cores: " + cores);

        initialized = true;
        LOGGER.info(NAME + " initialization complete");
    }

    /**
     * Shut down all SurvivalCore systems cleanly.
     * Called during MinecraftServer shutdown.
     */
    public static void shutdown() {
        if (!initialized) return;

        LOGGER.info("Shutting down " + NAME + "...");

        if (AsyncEntityTracker.isEnabled()) {
            AsyncEntityTracker.get().shutdown();
        }
        if (AsyncPathfinder.isEnabled()) {
            AsyncPathfinder.get().shutdown();
        }
        if (AsyncMobSpawner.isEnabled()) {
            AsyncMobSpawner.get().shutdown();
        }

        // Final performance report
        if (PerformanceMonitor.isEnabled()) {
            PerformanceMonitor.get().generateReport();
        }

        initialized = false;
        LOGGER.info(NAME + " shutdown complete");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
