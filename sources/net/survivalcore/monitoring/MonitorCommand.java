package net.survivalcore.monitoring;

import net.survivalcore.SurvivalCore;
import net.survivalcore.async.AsyncEntityTracker;
import net.survivalcore.async.AsyncPathfinder;
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
     */
    public static String[] generateReport() {
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
