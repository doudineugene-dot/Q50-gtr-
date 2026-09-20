package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * Вкладка ДВИГАТЕЛЬ. Фон — engine_bg_clean.png; здесь только стрелки RPM и
 * BOOST, ступицы, названия приборов, буква селектора и четыре значения в
 * карточках.
 */
public final class EnginePage implements Page {

    public String title() {
        return "ДВИГАТЕЛЬ";
    }

    public void draw(Canvas c, Theme t, Layout l, VehicleData d) {
        float cy = Layout.DIAL_CY;
        float r = Layout.DIAL_R;

        float rpmCx = l.dialCx(0);
        OemDial.needle(c, t, rpmCx, cy, r, d.rpm, 0f, 8000f, 6300f);
        OemDial.hub(c, t, rpmCx, cy, r);
        OemDial.caption(c, t, rpmCx, cy, l.captionDy(), "RPM", "x1000");
        // Селектор передач отдельным каналом не приходит, поэтому в кружке
        // стоит прочерк, а не выдуманная передача.
        OemDial.gear(c, t, rpmCx, cy, "—");

        float boostCx = l.dialCx(1);
        OemDial.needle(c, t, boostCx, cy, r, d.boostActual, -1f, 2f, 1.8f);
        OemDial.hub(c, t, boostCx, cy, r);
        OemDial.caption(c, t, boostCx, cy, l.captionDy(), "BOOST", "bar");

        tile(c, t, l, 0, d.coolantTemp, 0, "°C", 105f, 112f);
        tile(c, t, l, 1, d.oilTemp, 0, "°C", 125f, 138f);
        tile(c, t, l, 2, d.oilPressure, 1, "bar", Float.NaN, Float.NaN);
        tile(c, t, l, 3, d.intakeTemp, 0, "°C", 55f, 70f);
    }

    static void tile(Canvas c, Theme t, Layout l, int i,
                     com.q50gtr.plus.data.Channel ch, int decimals, String unit,
                     float warnFrom, float alertFrom) {
        OemTile.value(c, t, l.tileValueX(i), Layout.TILE_VALUE_BASE,
                ch, decimals, unit, warnFrom, alertFrom, 30f, 8f);
    }
}
