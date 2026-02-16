package net.survivalcore.admin;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import net.survivalcore.config.SurvivalCoreConfig;

/**
 * Sensitive action logging for server administrators.
 * Tracks explosions, fire spread, and entity damage for audit trails.
 */
public final class ActionLogger {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static ActionLogger instance;

    private final boolean logExplosions;
    private final boolean logFireSpread;
    private final boolean logEntityDamage;

    // Recent action log (bounded queue)
    private final ConcurrentLinkedQueue<String> recentActions = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_ACTIONS = 100;

    private ActionLogger(boolean logExplosions, boolean logFireSpread, boolean logEntityDamage) {
        this.logExplosions = logExplosions;
        this.logFireSpread = logFireSpread;
        this.logEntityDamage = logEntityDamage;
    }

    public static void init() {
        SurvivalCoreConfig config = SurvivalCoreConfig.get();
        instance = new ActionLogger(
            config.adminLogExplosions,
            config.adminLogFireSpread,
            config.adminLogEntityDamage
        );
    }

    public static ActionLogger get() {
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    /**
     * Log an explosion event.
     * @param worldName world where explosion occurred
     * @param x, y, z explosion coordinates
     * @param sourceName what caused the explosion
     * @param power explosion power
     */
    public void logExplosion(String worldName, double x, double y, double z, String sourceName, float power) {
        if (!logExplosions) return;
        String msg = String.format("[EXPLOSION] %s at %.0f,%.0f,%.0f in %s (power=%.1f)",
            sourceName, x, y, z, worldName, power);
        addAction(msg);
        LOGGER.info(msg);
    }

    /**
     * Log fire spread.
     * @param worldName world
     * @param x, y, z position
     */
    public void logFireSpread(String worldName, int x, int y, int z) {
        if (!logFireSpread) return;
        String msg = String.format("[FIRE] Spread at %d,%d,%d in %s", x, y, z, worldName);
        addAction(msg);
    }

    /**
     * Log entity block damage.
     * @param worldName world
     * @param entityName entity that caused damage
     * @param x, y, z position
     */
    public void logEntityDamage(String worldName, String entityName, int x, int y, int z) {
        if (!logEntityDamage) return;
        String msg = String.format("[ENTITY_DAMAGE] %s at %d,%d,%d in %s", entityName, x, y, z, worldName);
        addAction(msg);
    }

    private void addAction(String action) {
        recentActions.add(action);
        while (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions.poll();
        }
    }

    /**
     * Get recent actions for admin review.
     */
    public String[] getRecentActions(int count) {
        return recentActions.stream()
            .skip(Math.max(0, recentActions.size() - count))
            .toArray(String[]::new);
    }
}
