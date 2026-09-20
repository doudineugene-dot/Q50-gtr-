package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * One tab's worth of instruments. A page only reads VehicleData and draws —
 * it knows nothing about where the numbers came from.
 */
public interface Page {

    /** Tab caption. */
    String title();

    /**
     * Draw into the content box; the origin is already at its top-left.
     * {@code l} carries the measured grid for the current screen width.
     */
    void draw(Canvas c, Theme t, Layout l, VehicleData d);
}
