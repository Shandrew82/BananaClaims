package com.bananasandwich.bananaclaims.preview;

public final class PreviewSettings {

    public static final int COLOR_RGB = 0xA855F7;

    public static final float PARTICLE_SCALE = 1.70F;
    public static final float CORNER_PARTICLE_SCALE = 2.55F;
    public static final float SHADE_PARTICLE_SCALE = 0.95F;

    public static final int DURATION_TICKS = 20 * 20;
    public static final int REFRESH_INTERVAL_TICKS = 5;

    public static final int LINE_PARTICLE_COUNT = 3;
    public static final double LINE_PARTICLE_SPREAD = 0.045D;
    public static final double LINE_SPACING = 0.40D;

    public static final int CORNER_PARTICLE_COUNT = 7;
    public static final double CORNER_PARTICLE_SPREAD = 0.11D;

    /*
     * Side walls are intentionally much denser than the top and bottom
     * faces. This keeps the border readable while standing between corners.
     */
    public static final int SIDE_SHADE_PARTICLE_COUNT = 2;
    public static final double SIDE_SHADE_PARTICLE_SPREAD = 0.10D;
    public static final double SIDE_SHADE_SPACING = 0.85D;
    public static final int MAX_SIDE_SHADE_POINTS_PER_RENDER = 5200;

    public static final int HORIZONTAL_SHADE_PARTICLE_COUNT = 1;
    public static final double HORIZONTAL_SHADE_PARTICLE_SPREAD = 0.035D;
    public static final double HORIZONTAL_SHADE_SPACING = 2.00D;
    public static final int MAX_HORIZONTAL_SHADE_POINTS_PER_RENDER = 1800;

    private PreviewSettings() {
    }

    public static double calculateLineSpacing(
            double totalLineLength
    ) {
        return LINE_SPACING;
    }

    public static double calculateSideShadeSpacing(
            double totalVerticalSurfaceArea
    ) {
        return calculateCappedSpacing(
                totalVerticalSurfaceArea,
                SIDE_SHADE_SPACING,
                MAX_SIDE_SHADE_POINTS_PER_RENDER
        );
    }

    public static double calculateHorizontalShadeSpacing(
            double totalHorizontalSurfaceArea
    ) {
        return calculateCappedSpacing(
                totalHorizontalSurfaceArea,
                HORIZONTAL_SHADE_SPACING,
                MAX_HORIZONTAL_SHADE_POINTS_PER_RENDER
        );
    }

    private static double calculateCappedSpacing(
            double totalArea,
            double preferredSpacing,
            int maximumPoints
    ) {
        if (totalArea <= 0.0D) {
            return preferredSpacing;
        }

        double estimatedPoints =
                totalArea
                        / (preferredSpacing * preferredSpacing);

        if (estimatedPoints <= maximumPoints) {
            return preferredSpacing;
        }

        return Math.sqrt(
                totalArea / maximumPoints
        );
    }
}
