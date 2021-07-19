package com.aselab.rtls;

import android.view.MotionEvent;

import com.aselab.rtls.model.Point;

public interface PointSelectorListener {
    void onSelectPoint(Point point);
    boolean onTouchEvent(MotionEvent event);
}
