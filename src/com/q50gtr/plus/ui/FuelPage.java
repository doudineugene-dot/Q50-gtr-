package com.q50gtr.plus.ui;

import android.graphics.Paint;
import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * Fuel side: rail pressure, both banks' AFR, short and long term trims, and
 * boost target against actual.
 *
 * LPFP gets a cell of its own that says plainly it is not logged, rather than
 * a number nobody should trust.
 */
public final class FuelPage implements Page {

    private static final float MARGIN_X = 14f;
    private static final float GAP = 9f;

    public String title() {
        return "ТОПЛИВО";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float cw = (w - MARGIN_X * 2f - GAP * 3f) / 4f;
        float ch = (h - 8f - 18f - GAP * 2f) / 3f;
        float y0 = 8f;
        float y1 = y0 + ch + GAP;
        float y2 = y1 + ch + GAP;

        Gauges.tile(c, t, col(0, cw), y0, cw, ch, d.hpfpActual, 1, 0f, 25f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, col(1, cw), y0, cw, ch, d.hpfpTarget, 1, 0f, 25f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, col(2, cw), y0, cw, ch, d.boostActual, 2, -1f, 2f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, col(3, cw), y0, cw, ch, d.boostTarget, 2, -1f, 2f, Float.NaN, Float.NaN);

        Gauges.tile(c, t, col(0, cw), y1, cw, ch, d.afrB1, 2, 10f, 16f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, col(1, cw), y1, cw, ch, d.afrB2, 2, 10f, 16f, Float.NaN, Float.NaN);
        Gauges.tile(c, t, col(2, cw), y1, cw, ch, d.stftB1, 1, -25f, 25f, Float.NaN, Float.NaN, true, null);
        Gauges.tile(c, t, col(3, cw), y1, cw, ch, d.stftB2, 1, -25f, 25f, Float.NaN, Float.NaN, true, null);

        Gauges.tile(c, t, col(0, cw), y2, cw, ch, d.ltftB1, 1, -25f, 25f, Float.NaN, Float.NaN, true, null);
        Gauges.tile(c, t, col(1, cw), y2, cw, ch, d.ltftB2, 1, -25f, 25f, Float.NaN, Float.NaN, true, null);

        // LPFP: permanently unavailable in this EcuTek configuration.
        Gauges.tile(c, t, col(2, cw), y2, cw, ch, d.lpfp, 0, 0f, 0f, Float.NaN, Float.NaN,
                false, "LPFP");
        c.drawText("не логируется", col(2, cw) + cw / 2f, y2 + ch - 16f,
                t.text(Theme.DIM, 11f, Paint.Align.CENTER, false));

        notesCell(c, t, col(3, cw), y2, cw, ch);
    }

    private static float col(int i, float cw) {
        return MARGIN_X + i * (cw + GAP);
    }

    private void notesCell(Canvas c, Theme t, float x, float y, float w, float h) {
        Gauges.panel(c, t, x, y, w, h);
        c.drawText("ЧЕГО НЕТ В ЛОГЕ", x + 10f, y + 19f,
                t.text(Theme.SILVER, 12f, Paint.Align.LEFT, false));
        c.drawText("LPFP — низкое давление", x + 10f, y + 44f,
                t.text(Theme.DIM, 12f, Paint.Align.LEFT, false));
        c.drawText("Turbo Speed — не пишется", x + 10f, y + 62f,
                t.text(Theme.DIM, 12f, Paint.Align.LEFT, false));
        c.drawText("Значения не выдумываются", x + 10f, y + 84f,
                t.text(Theme.ACCENT, 11f, Paint.Align.LEFT, false));
    }
}
