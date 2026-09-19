package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/** RPM and boost on the dials, the four temperatures and pressures beside them. */
public final class EnginePage implements Page {

    public String title() {
        return "ДВИГАТЕЛЬ";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        Gauges.arcGauge(c, t, 148f, 178f, 136f, d.rpm,
                0f, 7000f, 0, 7, 6500f, "RPM", 1000f);
        c.drawText("x1000", 148f, 300f,
                t.text(Theme.DIM, 12f, android.graphics.Paint.Align.CENTER, false));

        Gauges.arcGauge(c, t, 370f, 178f, 106f, d.boostActual,
                -1f, 2f, 2, 6, Float.NaN, "BOOST", 1f);

        float tx = 492f;
        float tw = 160f;
        float th = 176f;
        Gauges.tile(c, t, tx, 6f, tw, th, d.coolantTemp, 0, 40f, 130f, 105f, 112f);
        Gauges.tile(c, t, tx + 174f, 6f, tw, th, d.oilTemp, 0, 40f, 150f, 125f, 138f);
        Gauges.tile(c, t, tx, 194f, tw, th, d.oilPressure, 1, 0f, 7f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, tx + 174f, 194f, tw, th, d.intakeTemp, 0, 0f, 90f, 55f, 70f);
    }
}
