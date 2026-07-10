package com.bananasandwich.bananaclaims.preview;

public record BoundarySurface(
        double originX,
        double originY,
        double originZ,
        double axisUX,
        double axisUY,
        double axisUZ,
        double axisVX,
        double axisVY,
        double axisVZ
) {
    public double width() {
        return Math.sqrt(
                axisUX * axisUX
                        + axisUY * axisUY
                        + axisUZ * axisUZ
        );
    }

    public double height() {
        return Math.sqrt(
                axisVX * axisVX
                        + axisVY * axisVY
                        + axisVZ * axisVZ
        );
    }

    public double area() {
        double crossX =
                axisUY * axisVZ
                        - axisUZ * axisVY;

        double crossY =
                axisUZ * axisVX
                        - axisUX * axisVZ;

        double crossZ =
                axisUX * axisVY
                        - axisUY * axisVX;

        return Math.sqrt(
                crossX * crossX
                        + crossY * crossY
                        + crossZ * crossZ
        );
    }

    public boolean isVertical() {
        return Math.abs(axisUY) > 0.0001D
                || Math.abs(axisVY) > 0.0001D;
    }

    public boolean isHorizontal() {
        return !isVertical();
    }
}

