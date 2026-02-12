package net.survivalcore.monitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Performance monitoring system for SurvivalCore.
 *
 * Tracks per-tick timing breakdowns:
 * - Entity processing time
 * - Block tick time
 * - Chunk operations
 * - Pathfinding time
 * - Entity tracking time
 * - Mob spawning time
 *
 * Also tracks async thread pool utilization.
 */
public final class PerformanceMonitor {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static volatile PerformanceMonitor instance;

    private final Map<String, TimingData> timings = new ConcurrentHashMap<>();
    private final int reportInterval;
    private int ticksSinceReport = 0;

    // Pre-defined timing categories
    public static final String ENTITIES = "entities";
    public static final String BLOCKS = "blocks";
    public static final String CHUNKS = "chunks";
    public static final String PATHFINDING = "pathfinding";
    public static final String ENTITY_TRACKING = "entity-tracking";
    public static final String MOB_SPAWNING = "mob-spawning";
    public static final String REDSTONE = "redstone";
    public static final String TICK_TOTAL = "tick-total";

    private PerformanceMonitor(int reportInterval) {
        this.reportInterval = reportInterval;

        // Initialize known categories
        timings.put(ENTITIES, new TimingData());
        timings.put(BLOCKS, new TimingData());
        timings.put(CHUNKS, new TimingData());
        timings.put(PATHFINDING, new TimingData());
        timings.put(ENTITY_TRACKING, new TimingData());
        timings.put(MOB_SPAWNING, new TimingData());
        timings.put(REDSTONE, new TimingData());
        timings.put(TICK_TOTAL, new TimingData());
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        if (!config.monitoringEnabled) {
            LOGGER.info("Performance monitoring is disabled");
            return;
        }

        instance = new PerformanceMonitor(config.monitoringReportInterval);
        LOGGER.info("Performance monitoring initialized (report interval: "
            + config.monitoringReportInterval + " ticks)");
    }

    public static PerformanceMonitor get() {
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    /**
     * Record a timing measurement for the given category.
     *
     * @param category the timing category
     * @param nanos    elapsed nanoseconds
     */
    public void record(String category, long nanos) {
        TimingData data = timings.get(category);
        if (data != null) {
            data.record(nanos);
        }
    }

    /**
     * Start a timing measurement. Returns the start time in nanos.
     */
    public static long startTiming() {
        return System.nanoTime();
    }

    /**
     * End a timing measurement and record it.
     */
    public void endTiming(String category, long startNanos) {
        record(category, System.nanoTime() - startNanos);
    }

    /**
     * Called once per server tick to check if we should generate a report.
     */
    public void tick() {
        if (reportInterval <= 0) return;

        ticksSinceReport++;
        if (ticksSinceReport >= reportInterval) {
            ticksSinceReport = 0;
            generateReport();
        }
    }

    /**
     * Get a snapshot of timing data for a category.
     */
    public TimingSnapshot getSnapshot(String category) {
        TimingData data = timings.get(category);
        if (data == null) return TimingSnapshot.EMPTY;
        return data.snapshot();
    }

    /**
     * Generate and log a performance report.
     */
    public void generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n=== SurvivalCore Performance Report ===\n");

        for (Map.Entry<String, TimingData> entry : timings.entrySet()) {
            TimingSnapshot snap = entry.getValue().snapshotAndReset();
            if (snap.count == 0) continue;

            report.append(String.format("  %-20s avg: %6.2fms  max: %6.2fms  count: %d\n",
                entry.getKey(),
                snap.avgMs(),
                snap.maxMs(),
                snap.count));
        }

        report.append("=======================================");
        LOGGER.info(report.toString());
    }

    /**
     * Thread-safe timing accumulator for a single category.
     */
    public static class TimingData {
        private final AtomicLong totalNanos = new AtomicLong(0);
        private final AtomicLong maxNanos = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);

        public void record(long nanos) {
            totalNanos.addAndGet(nanos);
            count.incrementAndGet();
            // Update max atomically
            long currentMax;
            do {
                currentMax = maxNanos.get();
                if (nanos <= currentMax) break;
            } while (!maxNanos.compareAndSet(currentMax, nanos));
        }

        public TimingSnapshot snapshot() {
            return new TimingSnapshot(
                totalNanos.get(),
                maxNanos.get(),
                count.get()
            );
        }

        public TimingSnapshot snapshotAndReset() {
            TimingSnapshot snap = snapshot();
            totalNanos.set(0);
            maxNanos.set(0);
            count.set(0);
            return snap;
        }
    }

    /**
     * Immutable snapshot of timing data.
     */
    public record TimingSnapshot(long totalNanos, long maxNanos, long count) {
        public static final TimingSnapshot EMPTY = new TimingSnapshot(0, 0, 0);

        public double avgMs() {
            if (count == 0) return 0.0;
            return (totalNanos / (double) count) / 1_000_000.0;
        }

        public double maxMs() {
            return maxNanos / 1_000_000.0;
        }

        public double totalMs() {
            return totalNanos / 1_000_000.0;
        }
    }
}
