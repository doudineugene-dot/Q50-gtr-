package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/** Экран 1 по docs/UI-MASTER-SPEC.md: RPM и BOOST, под ними четыре карточки. */
public final class EnginePage implements Page {

    public String title() {
        return "ДВИГАТЕЛЬ";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float cy = Layout.DIAL_CY;
        float r = Layout.DIAL_R;

        OemDial.draw(c, t, Layout.DIAL_LEFT_CX, cy, r, d.rpm,
                0f, 8000f, 0, 8, 6300f, "RPM", "x1000", 1000f, 0, false, null);
        // Селектор передач отдельным каналом не приходит, поэтому индикатор
        // под осью показывает прочерк, а не выдуманную передачу.
        OemDial.gearBadge(c, t, Layout.DIAL_LEFT_CX, cy, r, "—");

        OemDial.draw(c, t, Layout.DIAL_RIGHT_CX, cy, r, d.boostActual,
                -1f, 2f, 2, 6, 1.8f, "BOOST", "bar", 1f, 1, true, null);

        Layout.tile(c, t, 0, Icons.COOLANT, "ОХЛ. ЖИДКОСТЬ", d.coolantTemp, 0, "°C", 105f, 112f);
        Layout.tile(c, t, 1, Icons.OIL_TEMP, "ТЕМП. МАСЛА", d.oilTemp, 0, "°C", 125f, 138f);
        Layout.tile(c, t, 2, Icons.OIL_PRESS, "ДАВЛ. МАСЛА", d.oilPressure, 1, "bar", Float.NaN, Float.NaN);
        Layout.tile(c, t, 3, Icons.INTAKE, "ТЕМП. ВПУСКА", d.intakeTemp, 0, "°C", 55f, 70f);
    }
}
