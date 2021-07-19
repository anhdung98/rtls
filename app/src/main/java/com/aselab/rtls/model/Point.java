package com.aselab.rtls.model;

import android.graphics.Color;
import android.graphics.Paint;

public class Point {
    public enum PointShape {RECT, CIRC}
    public enum PointType {POINT, ANCHOR, TAG, TAG_TRAIL, TARGET}

    private double x;
    private double y;
    private String title;
    private int verticalAlignment;
    private int horizontalAlignment;
    private int color;
    private PointShape shape;
    private int size;
    private Paint paint;
    private PointType type = PointType.POINT;

    public Point() {
        this(0, 0, "Point");
    }

    public Point(double x, double y) {
        this(x, y, "Point");
    }

    public Point(double x, double y, String title) {
        this.x = x;
        this.y = y;
        this.title = title;
        this.verticalAlignment = -1;
        this.horizontalAlignment = 0;
        this.color = Color.rgb(97,97,97);
        this.shape = PointShape.RECT;
        this.size = 10;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public PointShape getShape() {
        return shape;
    }

    public void setShape(PointShape shape) {
        this.shape = shape;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public PointType getType() {
        return type;
    }
}
