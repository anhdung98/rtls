package com.aselab.rtls.model;

public class Tag extends Point {

    private PointType type = PointType.TAG;

    public Tag() {
        super(0, 0, "Tag");
    }

    public Tag(double x, double y) {
        super(x, y, "Tag");
    }

    public Tag(double x, double y, String title) {
        super(x, y, title);
    }

    @Override
    public PointType getType() {
        return type;
    }
}
