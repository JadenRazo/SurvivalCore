package net.survivalcore.math;

import java.util.logging.Logger;

/**
 * SIMD-accelerated operations using Java 21 Vector API.
 *
 * Operations that benefit from data-parallel execution:
 * - Map rendering: 8-wide color distance calculations
 * - Entity distance: Batch distance checks for multiple entities
 * - Chunk operations: Batch block state comparisons
 *
 * Automatically detects hardware SIMD support at runtime.
 * Falls back to scalar operations if Vector API is unavailable.
 */
public final class SIMDOperations {

    private static final Logger LOGGER = Logger.getLogger("SurvivalCore");
    private static boolean vectorApiAvailable = false;

    static {
        try {
            // Probe for Vector API availability
            Class.forName("jdk.incubator.vector.FloatVector");
            vectorApiAvailable = true;
            LOGGER.info("SIMD Vector API detected and enabled");
        } catch (ClassNotFoundException e) {
            LOGGER.info("SIMD Vector API not available (add --add-modules=jdk.incubator.vector to JVM flags)");
        }
    }

    private SIMDOperations() {}

    public static boolean isAvailable() {
        return vectorApiAvailable;
    }

    /**
     * Batch distance-squared calculations for entity distance checks.
     * Processes 4 entity positions at a time using SIMD when available.
     *
     * @param originX  reference X position
     * @param originY  reference Y position
     * @param originZ  reference Z position
     * @param entityX  array of entity X positions
     * @param entityY  array of entity Y positions
     * @param entityZ  array of entity Z positions
     * @param results  output array for distance-squared values
     * @param count    number of entities to process
     */
    public static void batchDistanceSq(
        double originX, double originY, double originZ,
        double[] entityX, double[] entityY, double[] entityZ,
        double[] results, int count
    ) {
        if (vectorApiAvailable) {
            batchDistanceSqSIMD(originX, originY, originZ, entityX, entityY, entityZ, results, count);
        } else {
            batchDistanceSqScalar(originX, originY, originZ, entityX, entityY, entityZ, results, count);
        }
    }

    /**
     * Scalar fallback for batch distance calculations.
     */
    private static void batchDistanceSqScalar(
        double originX, double originY, double originZ,
        double[] entityX, double[] entityY, double[] entityZ,
        double[] results, int count
    ) {
        for (int i = 0; i < count; i++) {
            double dx = entityX[i] - originX;
            double dy = entityY[i] - originY;
            double dz = entityZ[i] - originZ;
            results[i] = dx * dx + dy * dy + dz * dz;
        }
    }

    /**
     * SIMD-accelerated batch distance calculations.
     * This method is only loaded when Vector API is available.
     */
    private static void batchDistanceSqSIMD(
        double originX, double originY, double originZ,
        double[] entityX, double[] entityY, double[] entityZ,
        double[] results, int count
    ) {
        try {
            // Use reflection to avoid compile-time Vector API dependency
            Class<?> dvClass = Class.forName("jdk.incubator.vector.DoubleVector");
            Class<?> speciesClass = Class.forName("jdk.incubator.vector.VectorSpecies");
            Object species = dvClass.getField("SPECIES_256").get(null);
            int step = (int) speciesClass.getMethod("length").invoke(species); // 4

            int i = 0;
            for (; i + step <= count; i += step) {
                // vx = DoubleVector.fromArray(species, entityX, i).sub(originX)
                Object vx = dvClass.getMethod("fromArray", speciesClass, double[].class, int.class)
                    .invoke(null, species, entityX, i);
                vx = dvClass.getMethod("sub", double.class).invoke(vx, originX);

                // vy = DoubleVector.fromArray(species, entityY, i).sub(originY)
                Object vy = dvClass.getMethod("fromArray", speciesClass, double[].class, int.class)
                    .invoke(null, species, entityY, i);
                vy = dvClass.getMethod("sub", double.class).invoke(vy, originY);

                // vz = DoubleVector.fromArray(species, entityZ, i).sub(originZ)
                Object vz = dvClass.getMethod("fromArray", speciesClass, double[].class, int.class)
                    .invoke(null, species, entityZ, i);
                vz = dvClass.getMethod("sub", double.class).invoke(vz, originZ);

                // result = vx * vx + vy * vy + vz * vz
                Object result = dvClass.getMethod("mul", dvClass).invoke(vx, vx);
                Object vySquared = dvClass.getMethod("mul", dvClass).invoke(vy, vy);
                Object vzSquared = dvClass.getMethod("mul", dvClass).invoke(vz, vz);
                result = dvClass.getMethod("add", dvClass).invoke(result, vySquared);
                result = dvClass.getMethod("add", dvClass).invoke(result, vzSquared);

                // Store result
                dvClass.getMethod("intoArray", double[].class, int.class).invoke(result, results, i);
            }

            // Scalar tail for remaining elements
            for (; i < count; i++) {
                double dx = entityX[i] - originX;
                double dy = entityY[i] - originY;
                double dz = entityZ[i] - originZ;
                results[i] = dx * dx + dy * dy + dz * dz;
            }
        } catch (Exception e) {
            // Fallback to scalar if reflection fails
            LOGGER.warning("SIMD batch distance calculation failed, using scalar fallback: " + e.getMessage());
            batchDistanceSqScalar(originX, originY, originZ, entityX, entityY, entityZ, results, count);
        }
    }

    /**
     * Batch color distance calculations for map rendering.
     * Computes distances between a target color and an array of palette colors.
     * Processes 8 colors at a time using SIMD when available.
     *
     * @param targetR  target red component
     * @param targetG  target green component
     * @param targetB  target blue component
     * @param paletteR palette red components
     * @param paletteG palette green components
     * @param paletteB palette blue components
     * @param results  output array for distance values
     * @param count    number of palette entries
     */
    public static void batchColorDistance(
        float targetR, float targetG, float targetB,
        float[] paletteR, float[] paletteG, float[] paletteB,
        float[] results, int count
    ) {
        if (vectorApiAvailable) {
            batchColorDistanceSIMD(targetR, targetG, targetB, paletteR, paletteG, paletteB, results, count);
        } else {
            batchColorDistanceScalar(targetR, targetG, targetB, paletteR, paletteG, paletteB, results, count);
        }
    }

    /**
     * Scalar fallback for batch color distance calculations.
     */
    private static void batchColorDistanceScalar(
        float targetR, float targetG, float targetB,
        float[] paletteR, float[] paletteG, float[] paletteB,
        float[] results, int count
    ) {
        for (int i = 0; i < count; i++) {
            float dr = paletteR[i] - targetR;
            float dg = paletteG[i] - targetG;
            float db = paletteB[i] - targetB;
            results[i] = dr * dr + dg * dg + db * db;
        }
    }

    /**
     * SIMD-accelerated batch color distance calculations.
     * This method is only loaded when Vector API is available.
     */
    private static void batchColorDistanceSIMD(
        float targetR, float targetG, float targetB,
        float[] paletteR, float[] paletteG, float[] paletteB,
        float[] results, int count
    ) {
        try {
            // Use reflection to avoid compile-time Vector API dependency
            Class<?> fvClass = Class.forName("jdk.incubator.vector.FloatVector");
            Class<?> speciesClass = Class.forName("jdk.incubator.vector.VectorSpecies");
            Object species = fvClass.getField("SPECIES_256").get(null);
            int step = (int) speciesClass.getMethod("length").invoke(species); // 8

            int i = 0;
            for (; i + step <= count; i += step) {
                // vr = FloatVector.fromArray(species, paletteR, i).sub(targetR)
                Object vr = fvClass.getMethod("fromArray", speciesClass, float[].class, int.class)
                    .invoke(null, species, paletteR, i);
                vr = fvClass.getMethod("sub", float.class).invoke(vr, targetR);

                // vg = FloatVector.fromArray(species, paletteG, i).sub(targetG)
                Object vg = fvClass.getMethod("fromArray", speciesClass, float[].class, int.class)
                    .invoke(null, species, paletteG, i);
                vg = fvClass.getMethod("sub", float.class).invoke(vg, targetG);

                // vb = FloatVector.fromArray(species, paletteB, i).sub(targetB)
                Object vb = fvClass.getMethod("fromArray", speciesClass, float[].class, int.class)
                    .invoke(null, species, paletteB, i);
                vb = fvClass.getMethod("sub", float.class).invoke(vb, targetB);

                // result = vr * vr + vg * vg + vb * vb
                Object result = fvClass.getMethod("mul", fvClass).invoke(vr, vr);
                Object vgSquared = fvClass.getMethod("mul", fvClass).invoke(vg, vg);
                Object vbSquared = fvClass.getMethod("mul", fvClass).invoke(vb, vb);
                result = fvClass.getMethod("add", fvClass).invoke(result, vgSquared);
                result = fvClass.getMethod("add", fvClass).invoke(result, vbSquared);

                // Store result
                fvClass.getMethod("intoArray", float[].class, int.class).invoke(result, results, i);
            }

            // Scalar tail for remaining elements
            for (; i < count; i++) {
                float dr = paletteR[i] - targetR;
                float dg = paletteG[i] - targetG;
                float db = paletteB[i] - targetB;
                results[i] = dr * dr + dg * dg + db * db;
            }
        } catch (Exception e) {
            // Fallback to scalar if reflection fails
            LOGGER.warning("SIMD batch color distance calculation failed, using scalar fallback: " + e.getMessage());
            batchColorDistanceScalar(targetR, targetG, targetB, paletteR, paletteG, paletteB, results, count);
        }
    }

    /**
     * Find the minimum value index in a float array.
     * Used for nearest-color lookup in map rendering.
     */
    public static int findMinIndex(float[] values, int count) {
        int minIdx = 0;
        float minVal = values[0];
        for (int i = 1; i < count; i++) {
            if (values[i] < minVal) {
                minVal = values[i];
                minIdx = i;
            }
        }
        return minIdx;
    }
}
