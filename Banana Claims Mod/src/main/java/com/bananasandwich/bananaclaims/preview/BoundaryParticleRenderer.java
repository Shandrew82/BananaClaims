package com.bananasandwich.bananaclaims.preview;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public final class BoundaryParticleRenderer {

    private static final DustParticleOptions CORNER_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.CORNER_COLOR_RGB,
                    PreviewSettings.CORNER_PARTICLE_SCALE
            );

    private static final DustParticleOptions EDGE_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.EDGE_COLOR_RGB,
                    PreviewSettings.EDGE_PARTICLE_SCALE
            );

    private static final DustParticleOptions GUIDE_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.GUIDE_COLOR_RGB,
                    PreviewSettings.GUIDE_PARTICLE_SCALE
            );

    private static final DustParticleOptions WALL_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.WALL_COLOR_RGB,
                    PreviewSettings.WALL_PARTICLE_SCALE
            );

    private BoundaryParticleRenderer() {
    }

    public static void render(
            ServerPlayer player,
            BoundaryPreview preview
    ) {
        render(
                player,
                preview,
                PreviewSettings.MATERIALIZATION_TICKS
        );
    }

    public static void render(
            ServerPlayer player,
            BoundaryPreview preview,
            long elapsedTicks
    ) {
        if (player == null || preview == null) {
            return;
        }

        ServerLevel level = player.level();

        if (!level.dimension()
                .toString()
                .equals(preview.dimension())) {
            return;
        }

        double materialization =
                Math.min(
                        1.0D,
                        Math.max(
                                0.0D,
                                (double) elapsedTicks
                                        / PreviewSettings.MATERIALIZATION_TICKS
                        )
                );

        Set<PreviewPoint> corners =
                collectCorners(preview);

        /*
         * Strong corners are rendered first. During the opening animation,
         * vertical pillars grow upward before the full cage appears.
         */
        renderCornerMarkers(
                level,
                player,
                corners,
                elapsedTicks
        );

        renderStructuralEdges(
                level,
                player,
                preview,
                materialization
        );

        if (materialization >= 0.45D) {
            renderGuideBands(
                    level,
                    player,
                    preview,
                    materialization
            );
        }

        if (materialization >= 0.80D) {
            renderLocalWallIndication(
                    level,
                    player,
                    preview
            );
        }
    }

    private static Set<PreviewPoint> collectCorners(
            BoundaryPreview preview
    ) {
        Set<PreviewPoint> corners =
                new HashSet<>();

        for (BoundaryLine line : preview.lines()) {
            corners.add(
                    new PreviewPoint(
                            line.startX(),
                            line.startY(),
                            line.startZ()
                    )
            );

            corners.add(
                    new PreviewPoint(
                            line.endX(),
                            line.endY(),
                            line.endZ()
                    )
            );
        }

        return corners;
    }

    private static void renderCornerMarkers(
            ServerLevel level,
            ServerPlayer player,
            Set<PreviewPoint> corners,
            long elapsedTicks
    ) {
        double pulse =
                0.88D
                        + 0.22D
                        * Math.sin(elapsedTicks * 0.45D);

        for (PreviewPoint corner : corners) {
            if (!isWithinRenderRadius(
                    player,
                    corner.x(),
                    corner.z()
            )) {
                continue;
            }

            sendLongDistanceParticles(
                    level,
                    player,
                    CORNER_PARTICLE,
                    corner.x(),
                    corner.y(),
                    corner.z(),
                    PreviewSettings.CORNER_PARTICLE_COUNT,
                    PreviewSettings.CORNER_PARTICLE_SPREAD
                            * pulse
            );

            renderCornerHalo(
                    level,
                    player,
                    corner,
                    elapsedTicks
            );
        }
    }

    private static void renderCornerHalo(
            ServerLevel level,
            ServerPlayer player,
            PreviewPoint corner,
            long elapsedTicks
    ) {
        double rotation =
                elapsedTicks * 0.20D;

        double haloY =
                corner.y()
                        + PreviewSettings.CORNER_HALO_HEIGHT_OFFSET;

        for (int index = 0;
             index < PreviewSettings.CORNER_HALO_POINTS;
             index++) {
            double angle =
                    rotation
                            + (
                            Math.PI * 2.0D
                                    * index
                                    / PreviewSettings.CORNER_HALO_POINTS
                    );

            double x =
                    corner.x()
                            + Math.cos(angle)
                            * PreviewSettings.CORNER_HALO_RADIUS;

            double z =
                    corner.z()
                            + Math.sin(angle)
                            * PreviewSettings.CORNER_HALO_RADIUS;

            sendLongDistanceParticles(
                    level,
                    player,
                    CORNER_PARTICLE,
                    x,
                    haloY,
                    z,
                    1,
                    0.0D
            );
        }
    }

    private static void renderStructuralEdges(
            ServerLevel level,
            ServerPlayer player,
            BoundaryPreview preview,
            double materialization
    ) {
        for (BoundaryLine line : preview.lines()) {
            double reveal =
                    PreviewSettings.isVertical(line)
                            ? materialization
                            : Math.max(
                            0.0D,
                            (materialization - 0.30D)
                            / 0.70D
                    );

            if (reveal <= 0.0D) {
                continue;
            }

            renderLine(
                    level,
                    player,
                    line,
                    PreviewSettings.lineSpacing(line),
                    EDGE_PARTICLE,
                    PreviewSettings.EDGE_PARTICLE_COUNT,
                    PreviewSettings.EDGE_PARTICLE_SPREAD,
                    reveal
            );
        }
    }

    private static void renderGuideBands(
            ServerLevel level,
            ServerPlayer player,
            BoundaryPreview preview,
            double materialization
    ) {
        double reveal =
                Math.min(
                        1.0D,
                        (materialization - 0.45D)
                                / 0.55D
                );

        for (BoundarySurface surface : preview.surfaces()) {
            if (!surface.isVertical()) {
                continue;
            }

            double startY =
                    surface.originY();

            double endY =
                    surface.originY()
                            + surface.axisVY();

            double minY =
                    Math.min(startY, endY);

            double maxY =
                    Math.max(startY, endY);

            int firstBand =
                    (int) Math.ceil(
                            minY
                                    / PreviewSettings.GUIDE_BAND_INTERVAL
                    ) * PreviewSettings.GUIDE_BAND_INTERVAL;

            for (double y = firstBand;
                 y <= maxY;
                 y += PreviewSettings.GUIDE_BAND_INTERVAL) {
                renderLine(
                        level,
                        player,
                        horizontalLineAtY(
                                surface,
                                y
                        ),
                        PreviewSettings.GUIDE_LINE_SPACING,
                        GUIDE_PARTICLE,
                        PreviewSettings.GUIDE_PARTICLE_COUNT,
                        PreviewSettings.GUIDE_PARTICLE_SPREAD,
                        reveal
                );
            }
        }
    }

    private static void renderLocalWallIndication(
            ServerLevel level,
            ServerPlayer player,
            BoundaryPreview preview
    ) {
        int remainingBudget =
                PreviewSettings.MAX_LOCAL_WALL_POINTS_PER_RENDER;

        double playerMinY =
                player.getY()
                        - PreviewSettings.LOCAL_WALL_BELOW_PLAYER;

        double playerMaxY =
                player.getY()
                        + PreviewSettings.LOCAL_WALL_ABOVE_PLAYER;

        for (BoundarySurface surface : preview.surfaces()) {
            if (!surface.isVertical()
                    || remainingBudget <= 0) {
                continue;
            }

            remainingBudget =
                    renderLocalWallSurface(
                            level,
                            player,
                            surface,
                            playerMinY,
                            playerMaxY,
                            remainingBudget
                    );
        }
    }

    private static int renderLocalWallSurface(
            ServerLevel level,
            ServerPlayer player,
            BoundarySurface surface,
            double playerMinY,
            double playerMaxY,
            int remainingBudget
    ) {
        double surfaceStartY =
                surface.originY();

        double surfaceEndY =
                surface.originY()
                        + surface.axisVY();

        double minY =
                Math.max(
                        Math.min(
                                surfaceStartY,
                                surfaceEndY
                        ),
                        playerMinY
                );

        double maxY =
                Math.min(
                        Math.max(
                                surfaceStartY,
                                surfaceEndY
                        ),
                        playerMaxY
                );

        if (maxY <= minY) {
            return remainingBudget;
        }

        double width =
                surface.width();

        double visibleHeight =
                maxY - minY;

        int horizontalSteps =
                Math.max(
                        1,
                        (int) Math.ceil(
                                width
                                        / PreviewSettings.LOCAL_WALL_SPACING
                        )
                );

        int verticalSteps =
                Math.max(
                        1,
                        (int) Math.ceil(
                                visibleHeight
                                        / PreviewSettings.LOCAL_WALL_SPACING
                        )
                );

        for (int horizontal = 1;
             horizontal < horizontalSteps
                     && remainingBudget > 0;
             horizontal++) {
            double horizontalProgress =
                    (double) horizontal
                            / horizontalSteps;

            double x =
                    surface.originX()
                            + surface.axisUX()
                            * horizontalProgress;

            double z =
                    surface.originZ()
                            + surface.axisUZ()
                            * horizontalProgress;

            if (!isWithinRenderRadius(
                    player,
                    x,
                    z
            )) {
                continue;
            }

            for (int vertical = 1;
                 vertical < verticalSteps
                         && remainingBudget > 0;
                 vertical++) {
                double y =
                        minY
                                + visibleHeight
                                * vertical
                                / verticalSteps;

                sendLongDistanceParticles(
                        level,
                        player,
                        WALL_PARTICLE,
                        x,
                        y,
                        z,
                        PreviewSettings.LOCAL_WALL_PARTICLE_COUNT,
                        PreviewSettings.LOCAL_WALL_PARTICLE_SPREAD
                );

                remainingBudget--;
            }
        }

        return remainingBudget;
    }

    private static BoundaryLine horizontalLineAtY(
            BoundarySurface surface,
            double y
    ) {
        return new BoundaryLine(
                surface.originX(),
                y,
                surface.originZ(),
                surface.originX()
                        + surface.axisUX(),
                y,
                surface.originZ()
                        + surface.axisUZ()
        );
    }

    private static void renderLine(
            ServerLevel level,
            ServerPlayer player,
            BoundaryLine line,
            double spacing,
            DustParticleOptions particle,
            int particleCount,
            double particleSpread,
            double reveal
    ) {
        double length =
                line.length();

        int totalSteps =
                Math.max(
                        1,
                        (int) Math.ceil(length / spacing)
                );

        int visibleSteps =
                Math.max(
                        1,
                        (int) Math.ceil(
                                totalSteps
                                        * Math.min(
                                        1.0D,
                                        Math.max(
                                                0.0D,
                                                reveal
                                        )
                                )
                        )
                );

        for (int step = 0;
             step <= visibleSteps;
             step++) {
            double progress =
                    (double) step
                            / totalSteps;

            double x =
                    lerp(
                            line.startX(),
                            line.endX(),
                            progress
                    );

            double y =
                    lerp(
                            line.startY(),
                            line.endY(),
                            progress
                    );

            double z =
                    lerp(
                            line.startZ(),
                            line.endZ(),
                            progress
                    );

            if (!isWithinRenderRadius(
                    player,
                    x,
                    z
            )) {
                continue;
            }

            sendLongDistanceParticles(
                    level,
                    player,
                    particle,
                    x,
                    y,
                    z,
                    particleCount,
                    particleSpread
            );
        }
    }

    private static boolean isWithinRenderRadius(
            ServerPlayer player,
            double x,
            double z
    ) {
        double deltaX =
                x - player.getX();

        double deltaZ =
                z - player.getZ();

        return deltaX * deltaX
                + deltaZ * deltaZ
                <= PreviewSettings.RENDER_RADIUS_SQUARED;
    }

    private static void sendLongDistanceParticles(
            ServerLevel level,
            ServerPlayer player,
            DustParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double spread
    ) {
        level.sendParticles(
                player,
                particle,
                true,
                true,
                x,
                y,
                z,
                count,
                spread,
                spread,
                spread,
                0.0D
        );
    }

    private static double lerp(
            double start,
            double end,
            double progress
    ) {
        return start
                + (end - start)
                * progress;
    }

    private record PreviewPoint(
            double x,
            double y,
            double z
    ) {
    }
}




