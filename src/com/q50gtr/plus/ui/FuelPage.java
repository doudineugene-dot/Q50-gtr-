package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * Вкладка ТОПЛИВО. Фон — fuel_bg_clean.png.
 *
 * Прибор LPFP стоит на месте по эталону, но значения не показывает: низкое
 * давление топлива не логируется текущей конфигурацией EcuTek, и
 * правдоподобное число там было бы выдумкой. Стрелка в этом случае не
 * рисуется, под ступицей стоит «—» и «НЕТ ДАННЫХ».
 */
public final class FuelPage implements Page {

    public String title() {
        return "ТОПЛИВО";
    }

    public void draw(Canvas c, Theme t, Layout l, VehicleData d) {
        float cy = Layout.DIAL_CY;
        float r = Layout.DIAL_R;

        float lpfpCx = l.dialCx(0);
        OemDial.needle(c, t, lpfpCx, cy, r, d.lpfp, 0f, 10f, 9f);
        OemDial.hub(c, t, lpfpCx, cy, r);
        OemDial.caption(c, t, lpfpCx, cy, l.captionDy(), "LPFP", "bar");
        OemDial.value(c, t, lpfpCx, cy, d.lpfp, 1, "НЕТ ДАННЫХ");

        float hpfpCx = l.dialCx(1);
        OemDial.needle(c, t, hpfpCx, cy, r, d.hpfpActual, 0f, 250f, Float.NaN);
        OemDial.hub(c, t, hpfpCx, cy, r);
        OemDial.caption(c, t, hpfpCx, cy, l.captionDy(), "HPFP", "bar");
        OemDial.value(c, t, hpfpCx, cy, d.hpfpActual, 0, null);

        EnginePage.tile(c, t, l, 0, d.stftB1, 0, OemTile.SIGNED, Float.NaN, Float.NaN);
        EnginePage.tile(c, t, l, 1, d.afrB1, 1, "", Float.NaN, Float.NaN);
        EnginePage.tile(c, t, l, 2, d.boostTarget, 1, "bar", Float.NaN, Float.NaN);
        EnginePage.tile(c, t, l, 3, d.boostActual, 1, "bar", Float.NaN, Float.NaN);
    }
}
