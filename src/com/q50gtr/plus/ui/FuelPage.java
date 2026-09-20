package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * Экран 2 по docs/UI-MASTER-SPEC.md: LPFP и HPFP, под ними четыре карточки.
 *
 * Прибор LPFP стоит на месте по эталону, но значения не показывает: низкое
 * давление топлива не логируется текущей конфигурацией EcuTek, и правдоподобное
 * число там было бы выдумкой.
 */
public final class FuelPage implements Page {

    public String title() {
        return "ТОПЛИВО";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float cy = Layout.DIAL_CY;
        float r = Layout.DIAL_R;

        OemDial.draw(c, t, Layout.DIAL_LEFT_CX, cy, r, d.lpfp,
                0f, 10f, 1, 5, Float.NaN, "LPFP", "bar", 1f, 0, true, "НЕТ ДАННЫХ");
        OemDial.draw(c, t, Layout.DIAL_RIGHT_CX, cy, r, d.hpfpActual,
                0f, 250f, 0, 5, Float.NaN, "HPFP", "bar", 1f, 0, true, null);

        Layout.tile(c, t, 0, Icons.FUEL, "КОРРЕКЦИЯ ТОПЛИВА", d.stftB1, 1, "%", Float.NaN, Float.NaN);
        Layout.tile(c, t, 1, Icons.LAMBDA, "СМЕСЬ (AFR)", d.afrB1, 1, "", Float.NaN, Float.NaN);
        Layout.tile(c, t, 2, Icons.TURBO, "НАДДУВ (ЦЕЛЬ)", d.boostTarget, 1, "bar", Float.NaN, Float.NaN);
        Layout.tile(c, t, 3, Icons.TURBO, "НАДДУВ (ФАКТ)", d.boostActual, 1, "bar", Float.NaN, Float.NaN);
    }
}
