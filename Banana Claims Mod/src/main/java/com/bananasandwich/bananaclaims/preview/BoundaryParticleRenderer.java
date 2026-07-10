package com.bananasandwich.bananaclaims.preview;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public final class BoundaryParticleRenderer {

    private static final DustParticleOptions LINE_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.COLOR_RGB,
                    PreviewSettings.PARTICLE_SCALE
            );

    private static final DustParticleOptions CORNER_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.COLOR_RGB,
                    PreviewSettings.CORNER_PARTICLE_SCALE
            );

    private static final DustParticleOptions SHADE_PARTICLE =
            new DustParticleOptions(
                    PreviewSettings.COLOR_RGB,
                    PreviewSettings.SHADE_PARTICLE_SCALE
            );

    private BoundaryParticleRenderer() {
    }

    public static void render(
            ServerPlayer player,
            BoundaryPreview preview
    ) {
        if (player == null || preview == null) {
            return;
        }

        ServerLevel level = player.level();
        String currentDimension =
                level.dimension().toString();

        if (!currentDimension.equals(preview.dimension())) {
            return;
        }

        renderSurfaces(
                level,
                player,
                preview
        );

        double lineSpacing =
                PreviewSettings.calculateLineSpacing(
                        preview.totalLineLength()
                );

        Set<PreviewPoint> corners = new HashSet<>();

        for (BoundaryLine line : preview.lines()) {
            renderLine(
                    level,
                    player,
                    line,
                    lineSpacing
            );

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

        for (PreviewPoint corner : corners) {
            renderCorner(
                    level,
                    player,
                    corner
            );
        }
    }

    private static void renderSurfaces(
            ServerLevel level,
            ServerPlayer player,
            BoundaryPreview preview
    ) {
        if (preview.surfaces().isEmpty()) {
            return;
        }

        double sideSpacing =
                PreviewSettings.calculateSideShadeSpacing(
                        preview.totalVerticalSurfaceArea()
                );

        double horizontalSpacing =
                PreviewSettings.calculateHorizontalShadeSpacing(
                        preview.totalHorizontalSurfaceArea()
                );

        for (BoundarySurface surface : preview.surfaces()) {
            if (surface.isVertical()) {
                renderSurface(
                        level,
                        player,
                        surface,
                        sideSpacing,
                        PreviewSettings.SIDE_SHADE_PARTICLE_COUNT,
                        PreviewSettings.SIDE_SHADE_PARTICLE_SPREAD
                );
            } else {
                renderSurface(
                        level,
                        player,
                        surface,
                        horizontalSpacing,
                        PreviewSettings.HORIZONTAL_SHADE_PARTICLE_COUNT,
                        PreviewSettings.HORIZONTAL_SHADE_PARTICLE_SPREAD
                );
            }
        }
    }

    private static void renderSurface(
            ServerLevel level,
            ServerPlayer player,
            BoundarySurface surface,
            double spacing,
            int particleCount,
            double particleSpread
    ) {
        double width = surface.width();
        double height = surface.height();

        if (width <= 0.0D || height <= 0.0D) {
            return;
        }

        int stepsU = Math.max(
                1,
                (int) Math.ceil(width / spacing)
        );

        int stepsV = Math.max(
                1,
                (int) Math.ceil(height / spacing)
        );

        for (int u = 1; u < stepsU; u++) {
            double progressU =
                    (double) u / stepsU;

            for (int v = 1; v < stepsV; v++) {
                double progressV =
                        (double) v / stepsV;

                double x =
                        surface.originX()
                                + surface.axisUX() * progressU
                                + surface.axisVX() * progressV;

                double y =
                        surface.originY()
                                + surface.axisUY() * progressU
                                + surface.axisVY() * progressV;

                double z =
                        surface.originZ()
                                + surface.axisUZ() * progressU
                                + surface.axisVZ() * progressV;

                sendLongDistanceParticles(
                        level,
                        player,
                        SHADE_PARTICLE,
                        x,
                        y,
                        z,
                        particleCount,
                        particleSpread
                );
            }
        }
    }

    private static void renderLine(
            ServerLevel level,
            ServerPlayer player,
            BoundaryLine line,
            double spacing
    ) {
        double length = line.length();

        int steps = Math.max(
                1,
                (int) Math.ceil(length / spacing)
        );

        for (int step = 0; step <= steps; step++) {
            double progress =
                    (double) step / steps;

            double x = lerp(
                    line.startX(),
                    line.endX(),
                    progress
            );

            double y = lerp(
                    line.startY(),
                    line.endY(),
                    progress
            );

            double z = lerp(
                    line.startZ(),
                    line.endZ(),
                    progress
            );

            sendLongDistanceParticles(
                    level,
                    player,
                    LINE_PARTICLE,
                    x,
                    y,
                    z,
                    PreviewSettings.LINE_PARTICLE_COUNT,
                    PreviewSettings.LINE_PARTICLE_SPREAD
            );
        }
    }

    private static void renderCorner(
            ServerLevel level,
            ServerPlayer player,
            PreviewPoint corner
    ) {
        sendLongDistanceParticles(
                level,
                player,
                CORNER_PARTICLE,
                corner.x(),
                corner.y(),
                corner.z(),
                PreviewSettings.CORNER_PARTICLE_COUNT,
                PreviewSettings.CORNER_PARTICLE_SPREAD
        );
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
        /*
         * Both flags are enabled so Minecraft treats these preview
         * particles as forced/long-distance particles for the target player.
         */
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
                + (end - start) * progress;
    }

    private record PreviewPoint(
            double x,
            double y,
            double z
    ) {
    }
}


