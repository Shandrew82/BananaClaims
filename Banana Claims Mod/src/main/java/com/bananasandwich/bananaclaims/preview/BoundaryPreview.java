package com.bananasandwich.bananaclaims.preview;

import java.util.List;

public record BoundaryPreview(
        String dimension,
        List<BoundaryLine> lines,
        List<BoundarySurface> surfaces
) {
    public BoundaryPreview {
        lines = lines == null
                ? List.of()
                : List.copyOf(lines);

        surfaces = surfaces == null
                ? List.of()
                : List.copyOf(surfaces);
    }

    public BoundaryPreview(
            String dimension,
            List<BoundaryLine> lines
    ) {
        this(
                dimension,
                lines,
                List.of()
        );
    }

    public double totalLineLength() {
        return lines.stream()
                .mapToDouble(BoundaryLine::length)
                .sum();
    }

    public double totalVerticalSurfaceArea() {
        return surfaces.stream()
                .filter(BoundarySurface::isVertical)
                .mapToDouble(BoundarySurface::area)
                .sum();
    }

    public double totalHorizontalSurfaceArea() {
        return surfaces.stream()
                .filter(BoundarySurface::isHorizontal)
                .mapToDouble(BoundarySurface::area)
                .sum();
    }
}


