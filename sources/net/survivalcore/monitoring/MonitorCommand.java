package net.survivalcore.monitoring;

import net.survivalcore.SurvivalCore;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Implementation for the /survivalcore monitor command.
 *
 * Displays performance statistics, async thread pool status,
 * and configuration summary to operators.
 *
 * This is injected as a Bukkit command via the branding patch.
 */
public final class MonitorCommand {

    private MonitorCommand() {}

    /**
     * Generate the performance report as a formatted string array.
     * Each element is one line of output.
     *
     * @param subcommand optional subcommand (tps, entities, farms, threads) or null for overview
     */
    public static String[] generateReport(String subcommand) {
        if (subcommand == null || subcommand.isEmpty()) {
            return generateOverviewReport();
        }

        return switch (subcommand.toLowerCase()) {
            case "tps" -> generateTpsReport();
            case "entities" -> generateEntityReport();
            case "farms" -> generateFarmReport();
            case "threads" -> generateThreadReport();
            default -> generateOverviewReport();
        };
    }

    /**
     * Legacy method for backward compatibility.
     */
    public static String[] generateReport() {
        return generateOverviewReport();
    }

    /**
     * Generate the main overview report.
     */
    private static String[] generateOverviewReport() {
        StringBuilder sb = new StringBuilder();
        SurvivalCoreConfig config = SurvivalCoreConfig.get();

        sb.append("§6§l=== SurvivalCore v").append(SurvivalCore.VERSION).append(" ===§r\n");

        // Thread pool status
        sb.append("\n§e§lAsync Thread Pools:§r\n");
        sb.append("  §7Entity Tracker: §f")
            .append(config.asyncEntityTrackerEnabled ? "ON (" + config.resolveEntityTrackerThreads() + " threads)" : "OFF")
            .append("\n");
        sb.append("  §7Pathfinding: §f")
            .append(config.asyncPathfindingEnabled ? "ON (" + config.resolvePathfindingThreads() + " threads)" : "OFF")
            .append("\n");
        sb.append("  §7Mob Spawning: §f")
            .append(config.asyncMobSpawningEnabled ? "ON" : "OFF")
            .append("\n");

        // Performance flags
        sb.append("\n§e§lOptimizations:§r\n");
        sb.append("  §7FastMath: §f").append(config.fastMathEnabled ? "ON" : "OFF").append("\n");
        sb.append("  §7SIMD: §f").append(config.simdEnabled ? "ON" : "OFF").append("\n");
        sb.append("  §7Hopper Caching: §f").append(config.hopperOptimizedCaching ? "ON" : "OFF").append("\n");
        sb.append("  §7AI Throttling: §f").append(config.entityAiGoalSelectorThrottle ? "ON" : "OFF").append("\n");
        sb.append("  §7Object Pooling: §f").append(config.objectPoolingEnabled ? "ON" : "OFF").append("\n");
        sb.append("  §7Redstone: §f").append(config.redstoneImplementation).append("\n");
        sb.append("  §7Villager Lobotomize: §f").append(config.villagerLobotomizeEnabled ? "ON" : "OFF").append("\n");

        // New performance features
        sb.append("\n§e§lAdvanced Features:§r\n");
        sb.append("  §7DAB: §f").append(config.dabEnabled ? "ON (start: " + config.dabStartDistance + " chunks, max: " + config.dabMaxTickInterval + " ticks)" : "OFF").append("\n");
        sb.append("  §7Entity Budgets: §f").append(config.entityBudgetsEnabled ? "ON (" + config.entityBudgetsMaxEntityTimeMs + "ms/tick)" : "OFF").append("\n");
        sb.append("  §7Farm Detection: §f").append(config.farmDetectionEnabled ? "ON" : "OFF").append("\n");
        sb.append("  §7TNT Batching: §f").append(config.tntBatchingEnabled ? "ON" : "OFF").append("\n");
        sb.append("  §7Observer Debounce: §f").append(config.observerDebounceEnabled ? "ON" : "OFF").append("\n");

        // Exploit fixes
        sb.append("\n§e§lExploit Fixes:§r\n");
        sb.append("  §7Ender Pearl Border: §f").append(config.fixEnderPearlBorderBypass ? "FIXED" : "vanilla").append("\n");
        sb.append("  §7Chorus Fruit Wall Phase: §f").append(config.fixChorusFruitWallPhase ? "FIXED" : "vanilla").append("\n");
        sb.append("  §7Headless Piston: §f").append(config.fixHeadlessPiston ? "FIXED" : "vanilla").append("\n");
        sb.append("  §7TNT Dupe Limit: §f").append(config.fixTntDupeLimit == 0 ? "DISABLED" : config.fixTntDupeLimit + "/tick").append("\n");

        // QoL features
        sb.append("\n§e§lQuality of Life:§r\n");
        sb.append("  §7Single Player Sleep: §f").append(config.qolSinglePlayerSleep ? "ON" : "OFF").append("\n");
        sb.append("  §7Player Head Drops: §f").append(config.qolPlayerHeadDropsEnabled ? "ON" : "OFF").append("\n");
        sb.append("  §7Exp Multiplier: §f").append(config.qolExpMultiplier).append("x\n");

        // Performance timings
        if (PerformanceMonitor.isEnabled()) {
            PerformanceMonitor monitor = PerformanceMonitor.get();
            sb.append("\n§e§lTiming Averages:§r\n");

            appendTiming(sb, "Tick Total", monitor.getSnapshot(PerformanceMonitor.TICK_TOTAL));
            appendTiming(sb, "Entities", monitor.getSnapshot(PerformanceMonitor.ENTITIES));
            appendTiming(sb, "Blocks", monitor.getSnapshot(PerformanceMonitor.BLOCKS));
            appendTiming(sb, "Chunks", monitor.getSnapshot(PerformanceMonitor.CHUNKS));
            appendTiming(sb, "Pathfinding", monitor.getSnapshot(PerformanceMonitor.PATHFINDING));
            appendTiming(sb, "Entity Tracking", monitor.getSnapshot(PerformanceMonitor.ENTITY_TRACKING));
            appendTiming(sb, "Mob Spawning", monitor.getSnapshot(PerformanceMonitor.MOB_SPAWNING));
            appendTiming(sb, "Redstone", monitor.getSnapshot(PerformanceMonitor.REDSTONE));
        }

        // System info
        sb.append("\n§e§lSystem:§r\n");
        Runtime runtime = Runtime.getRuntime();
        sb.append("  §7CPU Cores: §f").append(runtime.availableProcessors()).append("\n");
        sb.append("  §7Memory: §f")
            .append(formatMB(runtime.totalMemory() - runtime.freeMemory()))
            .append(" / ")
            .append(formatMB(runtime.maxMemory()))
            .append(" MB\n");

        sb.append("\n§7Use §f/survivalcore monitor <tps|entities|farms|threads>§7 for details");

        return sb.toString().split("\n");
    }

    /**
     * Generate TPS-focused report.
     */
    private static String[] generateTpsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== TPS & Tick Timing Report ===§r\n");

        if (PerformanceMonitor.isEnabled()) {
            PerformanceMonitor monitor = PerformanceMonitor.get();

            sb.append("\n§e§lTick Breakdown:§r\n");
            appendTiming(sb, "Total Tick", monitor.getSnapshot(PerformanceMonitor.TICK_TOTAL));
            appendTiming(sb, "Entities", monitor.getSnapshot(PerformanceMonitor.ENTITIES));
            appendTiming(sb, "Blocks", monitor.getSnapshot(PerformanceMonitor.BLOCKS));
            appendTiming(sb, "Chunks", monitor.getSnapshot(PerformanceMonitor.CHUNKS));
            appendTiming(sb, "Redstone", monitor.getSnapshot(PerformanceMonitor.REDSTONE));

            sb.append("\n§e§lAsync Operations:§r\n");
            appendTiming(sb, "Pathfinding", monitor.getSnapshot(PerformanceMonitor.PATHFINDING));
            appendTiming(sb, "Entity Tracking", monitor.getSnapshot(PerformanceMonitor.ENTITY_TRACKING));
            appendTiming(sb, "Mob Spawning", monitor.getSnapshot(PerformanceMonitor.MOB_SPAWNING));

            sb.append("\n§e§lOptimization Impact:§r\n");
            long dabSkipped = monitor.getCounter(PerformanceMonitor.DAB_SKIPPED);
            long budgetDeferred = monitor.getCounter(PerformanceMonitor.BUDGET_DEFERRED);
            long mergesThrottled = monitor.getCounter(PerformanceMonitor.MERGES_THROTTLED);

            sb.append("  §7DAB Ticks Saved: §f").append(dabSkipped).append("\n");
            sb.append("  §7Budget Deferrals: §f").append(budgetDeferred).append("\n");
            sb.append("  §7Merges Throttled: §f").append(mergesThrottled).append("\n");
        } else {
            sb.append("§cMonitoring is disabled in config");
        }

        return sb.toString().split("\n");
    }

    /**
     * Generate entity-focused report.
     */
    private static String[] generateEntityReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== Entity Report ===§r\n");

        if (EntityReport.isEnabled()) {
            EntityReport report = EntityReport.get();
            sb.append("\n  §7Total Entities: §f").append(report.getTotalEntityCount()).append("\n");

            sb.append("\n§e§lBy World:§r\n");
            for (var entry : report.getCountsByWorld().entrySet()) {
                sb.append("  §7").append(entry.getKey()).append(": §f").append(entry.getValue()).append("\n");
            }

            sb.append("\n§e§lTop 10 Entity Types:§r\n");
            for (var entry : report.getTopEntityTypes(10).entrySet()) {
                sb.append("  §7").append(entry.getKey()).append(": §f").append(entry.getValue()).append("\n");
            }

            if (PerformanceMonitor.isEnabled()) {
                PerformanceMonitor monitor = PerformanceMonitor.get();
                long cleaned = monitor.getCounter(PerformanceMonitor.ENTITIES_CLEANED);
                sb.append("\n§e§lCleanup Stats:§r\n");
                sb.append("  §7Entities Cleaned: §f").append(cleaned).append("\n");
            }
        } else {
            sb.append("§cEntity reporting is disabled");
        }

        return sb.toString().split("\n");
    }

    /**
     * Generate farm detection report.
     */
    private static String[] generateFarmReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== Farm Detection Report ===§r\n");

        SurvivalCoreConfig config = SurvivalCoreConfig.get();

        sb.append("\n§e§lConfiguration:§r\n");
        sb.append("  §7Enabled: §f").append(config.farmDetectionEnabled ? "YES" : "NO").append("\n");
        sb.append("  §7Soft Threshold: §f").append(config.farmSoftThreshold).append(" entities\n");
        sb.append("  §7Hard Threshold: §f").append(config.farmHardThreshold).append(" entities\n");
        sb.append("  §7Critical Threshold: §f").append(config.farmCriticalThreshold).append(" entities\n");

        if (PerformanceMonitor.isEnabled()) {
            PerformanceMonitor monitor = PerformanceMonitor.get();
            long hotspots = monitor.getCounter(PerformanceMonitor.FARM_HOTSPOTS);

            sb.append("\n§e§lDetected Hotspots:§r\n");
            sb.append("  §7Total Hotspots: §f").append(hotspots).append("\n");

            if (hotspots > 0) {
                sb.append("\n§eHotspot details would require chunk scanning.\n");
                sb.append("§7Use §f/survivalcore farms scan§7 for detailed analysis.");
            }
        }

        return sb.toString().split("\n");
    }

    /**
     * Generate thread pool status report.
     */
    private static String[] generateThreadReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== Async Thread Pool Status ===§r\n");

        SurvivalCoreConfig config = SurvivalCoreConfig.get();

        sb.append("\n§e§lEntity Tracker:§r\n");
        sb.append("  §7Status: §f").append(config.asyncEntityTrackerEnabled ? "RUNNING" : "DISABLED").append("\n");
        if (config.asyncEntityTrackerEnabled) {
            sb.append("  §7Threads: §f").append(config.resolveEntityTrackerThreads()).append("\n");
            sb.append("  §7Compat Mode: §f").append(config.asyncEntityTrackerCompatMode ? "ON" : "OFF").append("\n");
        }

        sb.append("\n§e§lPathfinding:§r\n");
        sb.append("  §7Status: §f").append(config.asyncPathfindingEnabled ? "RUNNING" : "DISABLED").append("\n");
        if (config.asyncPathfindingEnabled) {
            sb.append("  §7Threads: §f").append(config.resolvePathfindingThreads()).append("\n");
        }

        sb.append("\n§e§lMob Spawning:§r\n");
        sb.append("  §7Status: §f").append(config.asyncMobSpawningEnabled ? "RUNNING" : "DISABLED").append("\n");

        sb.append("\n§e§lSystem Resources:§r\n");
        Runtime runtime = Runtime.getRuntime();
        sb.append("  §7Available Cores: §f").append(runtime.availableProcessors()).append("\n");
        sb.append("  §7Total Threads: §f").append(Thread.activeCount()).append("\n");

        return sb.toString().split("\n");
    }

    private static void appendTiming(StringBuilder sb, String name, PerformanceMonitor.TimingSnapshot snap) {
        if (snap.count() == 0) {
            sb.append("  §7").append(name).append(": §8no data\n");
            return;
        }
        String color = snap.avgMs() > 10 ? "§c" : snap.avgMs() > 5 ? "§e" : "§a";
        sb.append("  §7").append(name).append(": ")
            .append(color).append(String.format("%.2f", snap.avgMs())).append("ms")
            .append(" §8(max: ").append(String.format("%.2f", snap.maxMs())).append("ms)\n");
    }

    private static String formatMB(long bytes) {
        return String.valueOf(bytes / (1024 * 1024));
    }
}
