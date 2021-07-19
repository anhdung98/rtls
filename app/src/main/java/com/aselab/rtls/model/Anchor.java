package com.aselab.rtls.model;

public class Anchor extends Point {

    private PointType type = PointType.ANCHOR;

    public Anchor() {
        super(0, 0, "Anchor");
    }

    public Anchor(double x, double y) {
        super(x, y, "Anchor");
    }

    public Anchor(double x, double y, String title) {
        super(x, y, title);
    }

    @Override
    public PointType getType() {
        return type;
    }
}
