package com.bananasandwich.bananaclaims.preview;

public final class PreviewSettings {

    /*
     * Visual identity
     */
    public static final int CORNER_COLOR_RGB = 0xD8B4FE;
    public static final int EDGE_COLOR_RGB = 0xA855F7;
    public static final int GUIDE_COLOR_RGB = 0xC084FC;
    public static final int WALL_COLOR_RGB = 0x9333EA;

    public static final float CORNER_PARTICLE_SCALE = 3.10F;
    public static final float EDGE_PARTICLE_SCALE = 1.95F;
    public static final float GUIDE_PARTICLE_SCALE = 1.55F;
    public static final float WALL_PARTICLE_SCALE = 0.90F;

    /*
     * Session timing
     */
    public static final int DURATION_TICKS = 20 * 20;
    public static final int REFRESH_INTERVAL_TICKS = 10;
    public static final int MATERIALIZATION_TICKS = 12;

    /*
     * Structural frame. These layers always render before optional wall fill.
     */
    public static final int EDGE_PARTICLE_COUNT = 2;
    public static final double EDGE_PARTICLE_SPREAD = 0.035D;
    public static final double HORIZONTAL_EDGE_SPACING = 0.42D;
    public static final double VERTICAL_EDGE_SPACING = 0.90D;

    public static final int CORNER_PARTICLE_COUNT = 9;
    public static final double CORNER_PARTICLE_SPREAD = 0.13D;

    public static final int CORNER_HALO_POINTS = 12;
    public static final double CORNER_HALO_RADIUS = 0.72D;
    public static final double CORNER_HALO_HEIGHT_OFFSET = 1.25D;

    /*
     * Full-height guide bands make the claim readable away from its corners.
     */
    public static final int GUIDE_BAND_INTERVAL = 16;
    public static final double GUIDE_LINE_SPACING = 0.70D;
    public static final int GUIDE_PARTICLE_COUNT = 2;
    public static final double GUIDE_PARTICLE_SPREAD = 0.045D;

    /*
     * Dense wall indication follows the player's vertical position.
     * It is intentionally the first layer constrained by the particle budget.
     */
    public static final int LOCAL_WALL_BELOW_PLAYER = 20;
    public static final int LOCAL_WALL_ABOVE_PLAYER = 44;
    public static final double LOCAL_WALL_SPACING = 1.35D;
    public static final int LOCAL_WALL_PARTICLE_COUNT = 1;
    public static final double LOCAL_WALL_PARTICLE_SPREAD = 0.065D;
    public static final int MAX_LOCAL_WALL_POINTS_PER_RENDER = 1150;

    /*
     * Long-distance packets are used, but geometry outside this radius is not
     * generated. This protects clients when previewing very large claims.
     */
    public static final double RENDER_RADIUS = 144.0D;
    public static final double RENDER_RADIUS_SQUARED =
            RENDER_RADIUS * RENDER_RADIUS;

    private PreviewSettings() {
    }

    public static double lineSpacing(
            BoundaryLine line
    ) {
        return isVertical(line)
                ? VERTICAL_EDGE_SPACING
                : HORIZONTAL_EDGE_SPACING;
    }

    public static boolean isVertical(
            BoundaryLine line
    ) {
        return Math.abs(line.endY() - line.startY()) > 0.0001D
                && Math.abs(line.endX() - line.startX()) < 0.0001D
                && Math.abs(line.endZ() - line.startZ()) < 0.0001D;
    }
}


