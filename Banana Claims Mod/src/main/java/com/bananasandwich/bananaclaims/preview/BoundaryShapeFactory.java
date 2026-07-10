package com.bananasandwich.bananaclaims.preview;

import com.bananasandwich.bananaclaims.claim.Claim;
import com.bananasandwich.bananaclaims.claim.ClaimChunk;
import com.bananasandwich.bananaclaims.selection.ClaimSelection;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BoundaryShapeFactory {

    private BoundaryShapeFactory() {
    }

    public static BoundaryPreview fromSelection(
            ClaimSelection selection,
            int maximumBuildHeight
    ) {
        if (selection == null
                || !selection.hasBothPositions()
                || !selection.isSameDimension()) {
            return null;
        }

        BlockPos pos1 = selection.getPos1();
        BlockPos pos2 = selection.getPos2();

        double minX =
                Math.min(pos1.getX(), pos2.getX());

        double maxX =
                Math.max(pos1.getX(), pos2.getX())
                        + 1.0D;

        /*
         * The lower selected Y-level remains the floor. The preview now
         * reaches the dimension's build ceiling, matching claim previews.
         */
        double minY =
                Math.min(pos1.getY(), pos2.getY());

        double maxY = Math.max(
                minY + 1.0D,
                maximumBuildHeight
        );

        double minZ =
                Math.min(pos1.getZ(), pos2.getZ());

        double maxZ =
                Math.max(pos1.getZ(), pos2.getZ())
                        + 1.0D;

        return new BoundaryPreview(
                selection.getPos1Dimension(),
                createCuboidLines(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                ),
                createCuboidSurfaces(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                )
        );
    }

    public static BoundaryPreview fromSelection(
            ClaimSelection selection
    ) {
        if (selection == null
                || !selection.hasBothPositions()) {
            return null;
        }

        int selectedMaximumY =
                Math.max(
                        selection.getPos1().getY(),
                        selection.getPos2().getY()
                ) + 1;

        return fromSelection(
                selection,
                selectedMaximumY
        );
    }

    public static BoundaryPreview fromClaim(
            Claim claim,
            int minBuildHeight,
            int maxBuildHeight
    ) {
        if (claim == null
                || claim.getChunks().isEmpty()) {
            return null;
        }

        double minY = minBuildHeight;
        double maxY = maxBuildHeight;

        Set<HorizontalEdge> boundaryEdges =
                createClaimBoundaryEdges(claim);

        if (boundaryEdges.isEmpty()) {
            return null;
        }

        List<BoundaryLine> lines = new ArrayList<>();
        List<BoundarySurface> surfaces =
                new ArrayList<>();

        Set<Point> vertices =
                new LinkedHashSet<>();

        for (HorizontalEdge edge : boundaryEdges) {
            vertices.add(edge.start());
            vertices.add(edge.end());

            lines.add(
                    new BoundaryLine(
                            edge.start().x(),
                            minY,
                            edge.start().z(),
                            edge.end().x(),
                            minY,
                            edge.end().z()
                    )
            );

            lines.add(
                    new BoundaryLine(
                            edge.start().x(),
                            maxY,
                            edge.start().z(),
                            edge.end().x(),
                            maxY,
                            edge.end().z()
                    )
            );

            surfaces.add(
                    createVerticalWall(
                            edge,
                            minY,
                            maxY
                    )
            );
        }

        for (Point vertex : vertices) {
            lines.add(
                    new BoundaryLine(
                            vertex.x(),
                            minY,
                            vertex.z(),
                            vertex.x(),
                            maxY,
                            vertex.z()
                    )
            );
        }

        for (ClaimChunk chunk : claim.getChunks()) {
            double minX =
                    chunk.getChunkX() * 16.0D;

            double minZ =
                    chunk.getChunkZ() * 16.0D;

            surfaces.add(
                    createHorizontalFace(
                            minX,
                            minY,
                            minZ,
                            16.0D,
                            16.0D
                    )
            );

            surfaces.add(
                    createHorizontalFace(
                            minX,
                            maxY,
                            minZ,
                            16.0D,
                            16.0D
                    )
            );
        }

        return new BoundaryPreview(
                claim.getDimension(),
                lines,
                surfaces
        );
    }

    private static List<BoundaryLine> createCuboidLines(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        return List.of(
                new BoundaryLine(
                        minX, minY, minZ,
                        maxX, minY, minZ
                ),
                new BoundaryLine(
                        maxX, minY, minZ,
                        maxX, minY, maxZ
                ),
                new BoundaryLine(
                        maxX, minY, maxZ,
                        minX, minY, maxZ
                ),
                new BoundaryLine(
                        minX, minY, maxZ,
                        minX, minY, minZ
                ),

                new BoundaryLine(
                        minX, maxY, minZ,
                        maxX, maxY, minZ
                ),
                new BoundaryLine(
                        maxX, maxY, minZ,
                        maxX, maxY, maxZ
                ),
                new BoundaryLine(
                        maxX, maxY, maxZ,
                        minX, maxY, maxZ
                ),
                new BoundaryLine(
                        minX, maxY, maxZ,
                        minX, maxY, minZ
                ),

                new BoundaryLine(
                        minX, minY, minZ,
                        minX, maxY, minZ
                ),
                new BoundaryLine(
                        maxX, minY, minZ,
                        maxX, maxY, minZ
                ),
                new BoundaryLine(
                        maxX, minY, maxZ,
                        maxX, maxY, maxZ
                ),
                new BoundaryLine(
                        minX, minY, maxZ,
                        minX, maxY, maxZ
                )
        );
    }

    private static List<BoundarySurface> createCuboidSurfaces(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double sizeX = maxX - minX;
        double sizeY = maxY - minY;
        double sizeZ = maxZ - minZ;

        return List.of(
                new BoundarySurface(
                        minX,
                        minY,
                        minZ,
                        sizeX,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D,
                        sizeZ
                ),
                new BoundarySurface(
                        minX,
                        maxY,
                        minZ,
                        sizeX,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D,
                        sizeZ
                ),
                new BoundarySurface(
                        minX,
                        minY,
                        minZ,
                        sizeX,
                        0.0D,
                        0.0D,
                        0.0D,
                        sizeY,
                        0.0D
                ),
                new BoundarySurface(
                        minX,
                        minY,
                        maxZ,
                        sizeX,
                        0.0D,
                        0.0D,
                        0.0D,
                        sizeY,
                        0.0D
                ),
                new BoundarySurface(
                        minX,
                        minY,
                        minZ,
                        0.0D,
                        0.0D,
                        sizeZ,
                        0.0D,
                        sizeY,
                        0.0D
                ),
                new BoundarySurface(
                        maxX,
                        minY,
                        minZ,
                        0.0D,
                        0.0D,
                        sizeZ,
                        0.0D,
                        sizeY,
                        0.0D
                )
        );
    }

    private static BoundarySurface createVerticalWall(
            HorizontalEdge edge,
            double minY,
            double maxY
    ) {
        return new BoundarySurface(
                edge.start().x(),
                minY,
                edge.start().z(),
                edge.end().x()
                        - edge.start().x(),
                0.0D,
                edge.end().z()
                        - edge.start().z(),
                0.0D,
                maxY - minY,
                0.0D
        );
    }

    private static BoundarySurface createHorizontalFace(
            double minX,
            double y,
            double minZ,
            double sizeX,
            double sizeZ
    ) {
        return new BoundarySurface(
                minX,
                y,
                minZ,
                sizeX,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                sizeZ
        );
    }

    private static Set<HorizontalEdge> createClaimBoundaryEdges(
            Claim claim
    ) {
        Set<HorizontalEdge> edges =
                new HashSet<>();

        for (ClaimChunk chunk : claim.getChunks()) {
            int minX =
                    chunk.getChunkX() * 16;

            int minZ =
                    chunk.getChunkZ() * 16;

            int maxX = minX + 16;
            int maxZ = minZ + 16;

            toggleEdge(
                    edges,
                    new Point(minX, minZ),
                    new Point(maxX, minZ)
            );

            toggleEdge(
                    edges,
                    new Point(maxX, minZ),
                    new Point(maxX, maxZ)
            );

            toggleEdge(
                    edges,
                    new Point(maxX, maxZ),
                    new Point(minX, maxZ)
            );

            toggleEdge(
                    edges,
                    new Point(minX, maxZ),
                    new Point(minX, minZ)
            );
        }

        return edges;
    }

    private static void toggleEdge(
            Set<HorizontalEdge> edges,
            Point start,
            Point end
    ) {
        HorizontalEdge edge =
                new HorizontalEdge(start, end);

        HorizontalEdge reverse =
                new HorizontalEdge(end, start);

        if (!edges.remove(reverse)) {
            edges.add(edge);
        }
    }

    private record Point(
            int x,
            int z
    ) {
    }

    private record HorizontalEdge(
            Point start,
            Point end
    ) {
    }
}



