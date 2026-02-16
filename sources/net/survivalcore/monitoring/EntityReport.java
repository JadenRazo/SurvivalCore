package net.survivalcore.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Entity tracking and reporting for admin visibility.
 * Aggregates entities by type and world for the monitor command.
 */
public final class EntityReport {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static EntityReport instance;

    private final ConcurrentHashMap<String, Integer> entityCountsByType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> entityCountsByWorld = new ConcurrentHashMap<>();
    private int totalEntityCount = 0;

    private EntityReport() {}

    public static void init() {
        instance = new EntityReport();
    }

    public static EntityReport get() {
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    /**
     * Reset counts at the start of each reporting cycle.
     */
    public void reset() {
        entityCountsByType.clear();
        entityCountsByWorld.clear();
        totalEntityCount = 0;
    }

    /**
     * Track an entity for reporting.
     * @param typeName entity type simple name (e.g., "Zombie", "Villager")
     * @param worldName world name
     */
    public void trackEntity(String typeName, String worldName) {
        entityCountsByType.merge(typeName, 1, Integer::sum);
        entityCountsByWorld.merge(worldName, 1, Integer::sum);
        totalEntityCount++;
    }

    public int getTotalEntityCount() {
        return totalEntityCount;
    }

    public Map<String, Integer> getCountsByType() {
        return new HashMap<>(entityCountsByType);
    }

    public Map<String, Integer> getCountsByWorld() {
        return new HashMap<>(entityCountsByWorld);
    }

    /**
     * Get top N entity types by count.
     */
    public Map<String, Integer> getTopEntityTypes(int n) {
        return entityCountsByType.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(n)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, java.util.LinkedHashMap::new
            ));
    }

    /**
     * Generate formatted report lines.
     */
    public String[] generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e§lEntity Report:§r\n");
        sb.append("  §7Total: §f").append(totalEntityCount).append("\n");

        sb.append("  §7By World:§r\n");
        for (var entry : entityCountsByWorld.entrySet()) {
            sb.append("    §7").append(entry.getKey()).append(": §f").append(entry.getValue()).append("\n");
        }

        sb.append("  §7Top Types:§r\n");
        for (var entry : getTopEntityTypes(10).entrySet()) {
            sb.append("    §7").append(entry.getKey()).append(": §f").append(entry.getValue()).append("\n");
        }

        return sb.toString().split("\n");
    }
}
