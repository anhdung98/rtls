package com.aselab.rtls.model;

public class Target extends Point {

    private PointType type = PointType.TARGET;

    public Target() {
        super(0, 0, "Target");
    }

    public Target(double x, double y) {
        super(x, y, "Target");
    }

    public Target(double x, double y, String title) {
        super(x, y, title);
    }

    @Override
    public PointType getType() {
        return type;
    }

    public static Target fromPoint(Point point) {
        return new Target(point.getX(), point.getY());
    }
}
