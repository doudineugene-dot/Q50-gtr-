package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * The car seen from above, with air pressure at each corner wired to the wheel
 * it belongs to, plus driveline temperatures, board voltage and ambient temp.
 */
public final class ChassisPage implements Page {

    private static final float CAR_X = 350f;
    private static final float CAR_Y = 10f;
    private static final float CAR_W = 140f;
    private static final float CAR_H = 250f;

    public String title() {
        return "ШАССИ";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float colW = 152f;
        float leftX = 14f;
        float rightX = w - 14f - colW;
        float rowH = 112f;
        float r0 = 6f;
        float r1 = 126f;
        float r2 = 246f;

        // Geometry of the wheels the corner readouts point at.
        float carCx = CAR_X + CAR_W / 2f;
        float track = CAR_W * 0.335f;
        float axleFront = CAR_Y + CAR_H * 0.235f;
        float axleRear = CAR_Y + CAR_H * 0.745f;
        Q50Silhouette.draw(c, t, CAR_X, CAR_Y, CAR_W, CAR_H);

        leader(c, t, leftX + colW, r0 + rowH / 2f, carCx - track, axleFront);
        leader(c, t, rightX, r0 + rowH / 2f, carCx + track, axleFront);
        leader(c, t, leftX + colW, r1 + rowH / 2f, carCx - track, axleRear);
        leader(c, t, rightX, r1 + rowH / 2f, carCx + track, axleRear);

        Q50Silhouette.caption(c, t, carCx, CAR_Y + CAR_H + 16f, "AIRLIFT PERFORMANCE 3H");

        Gauges.tile(c, t, leftX, r0, colW, rowH, d.airFrontLeft, 0, 0f, 150f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, rightX, r0, colW, rowH, d.airFrontRight, 0, 0f, 150f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, leftX, r1, colW, rowH, d.airRearLeft, 0, 0f, 150f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, rightX, r1, colW, rowH, d.airRearRight, 0, 0f, 150f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, leftX, r2, colW, rowH, d.airTank, 0, 0f, 200f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, rightX, r2, colW, rowH, d.transmissionTemp, 0, 40f, 140f, 110f, 125f);

        float bw = 154f;
        float by = 270f;
        float bh = 100f;
        Gauges.tile(c, t, 178f, by, bw, bh, d.transferCaseTemp, 0, 40f, 140f, 110f, 125f);
        Gauges.tile(c, t, 342f, by, bw, bh, d.batteryVoltage, 1, 10f, 16f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, 506f, by, bw, bh, d.ambientTemp, 0, -30f, 50f, Float.NaN, Float.NaN);
    }

    private static void leader(Canvas c, Theme t, float x1, float y1, float x2, float y2) {
        c.drawLine(x1, y1, x2, y2, t.stroke(Theme.EDGE_SOFT, 1f));
        c.drawCircle(x2, y2, 2.5f, t.fill(Theme.ACCENT_DEEP));
    }
}
