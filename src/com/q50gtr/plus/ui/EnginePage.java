package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleData;

/** Вкладка ДВИГАТЕЛЬ: RPM и BOOST, под ними четыре карточки. */
public final class EnginePage implements Page {

    private final NeedleMotion rpmMotion = new NeedleMotion();
    private final NeedleMotion boostMotion = new NeedleMotion();

    public String title() {
        return "ДВИГАТЕЛЬ";
    }

    public void draw(Canvas c, Theme t, Layout l, VehicleData d) {
        float cy = l.dialCy;
        float r = l.dialR;
        long now = System.currentTimeMillis();

        OemDial.draw(c, t, l.dialCx(0), cy, r, d.rpm,
                0f, 8000f, 0, 8, 1, 6300f, "RPM", "x1000", 1000f, 0, false, null,
                rpmMotion.update(d.rpm, 0f, 8000f, now));
        // Селектор передач отдельным каналом не приходит, поэтому в кружке
        // прочерк, а не выдуманная передача.
        OemDial.gearBadge(c, t, l.dialCx(0), cy, r, "—");

        OemDial.draw(c, t, l.dialCx(1), cy, r, d.boostActual,
                -1f, 2f, 2, 6, 1, 1.8f, "BOOST", "bar", 1f, 1, false, null,
                boostMotion.update(d.boostActual, -1f, 2f, now));

        tile(c, t, l, 0, Icons.COOLANT, "ОХЛ. ЖИДКОСТЬ", d.coolantTemp, 0, "°C", 105f, 112f);
        tile(c, t, l, 1, Icons.OIL_TEMP, "ТЕМП. МАСЛА", d.oilTemp, 0, "°C", 125f, 138f);
        tile(c, t, l, 2, Icons.OIL_PRESS, "ДАВЛ. МАСЛА", d.oilPressure, 1, "bar", Float.NaN, Float.NaN);
        tile(c, t, l, 3, Icons.INTAKE, "ТЕМП. ВПУСКА", d.intakeTemp, 0, "°C", 55f, 70f);
    }

    static void tile(Canvas c, Theme t, Layout l, int i, int icon, String label,
                     Channel ch, int decimals, String unit,
                     float warnFrom, float alertFrom) {
        OemTile.draw(c, t, l.tileX(i), l.tileY, l.tileW, l.tileH,
                icon, label, ch, decimals, unit, warnFrom, alertFrom);
    }
}
