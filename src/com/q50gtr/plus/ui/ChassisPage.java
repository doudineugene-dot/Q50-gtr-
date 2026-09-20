package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleData;

/**
 * Вкладка ШАССИ: слева Q50 сзади с четырьмя стойками пневмоподвески, справа
 * колонка из четырёх карточек, внизу общий ряд.
 */
public final class ChassisPage implements Page {

    public String title() {
        return "ШАССИ";
    }

    public void draw(Canvas c, Theme t, Layout l, VehicleData d) {
        float s = l.s;
        float leftW = l.chassisSplitX();
        float k = leftW / 345f;

        c.drawText("ДАВЛЕНИЕ ПНЕВМОПОДВЕСКИ (bar)", leftW * 0.52f, 78f * s,
                t.text(Theme.LABEL, l.text(14f), Paint.Align.CENTER, false));

        float carX = 69f * k;
        float carW = 198f * k;
        if (Sprites.hasCar()) {
            Sprites.car(c, carX, 125f * s, carW, 138f * s);
        } else {
            Q50Rear.car(c, t, carX, 125f * s, carW, 138f * s);
        }

        float strutL = 44f * k;
        float strutR = 296f * k;
        float strutH = 59f * s;
        Q50Rear.strut(c, t, strutL, 158f * s, strutH, d.airFrontLeft, true);
        Q50Rear.strut(c, t, strutR, 158f * s, strutH, d.airFrontRight, true);
        Q50Rear.strut(c, t, strutL, 219f * s, strutH, d.airRearLeft, false);
        Q50Rear.strut(c, t, strutR, 219f * s, strutH, d.airRearRight, false);

        float colX = l.chassisColX();
        float colW = l.chassisColW();
        float rh = l.chassisCardH();
        OemTile.row(c, t, colX, l.chassisCardY(0), colW, rh, Icons.GEARBOX,
                "ТЕМП. АКПП", d.transmissionTemp, 0, "°C", 110f, 125f);
        OemTile.row(c, t, colX, l.chassisCardY(1), colW, rh, Icons.GEARBOX,
                "ТЕМП. РАЗДАТКИ", d.transferCaseTemp, 0, "°C", 110f, 125f);
        OemTile.row(c, t, colX, l.chassisCardY(2), colW, rh, Icons.BATTERY,
                "НАПРЯЖЕНИЕ", d.batteryVoltage, 1, "V", Float.NaN, Float.NaN);
        OemTile.row(c, t, colX, l.chassisCardY(3), colW, rh, Icons.AMBIENT,
                "ТЕМП. ОКР. ВОЗДУХА", d.ambientTemp, 0, "°C", Float.NaN, Float.NaN);

        Channel knock = d.maxKnockIndexChannel();
        tile(c, t, l, 0, Icons.SPARK, "УГОЛ ЗАЖИГАНИЯ", d.ignitionTiming, 0, "°", Float.NaN, Float.NaN);
        tile(c, t, l, 1, Icons.KNOCK, "KNOCK INDEX (MAX)",
                knock != null ? knock : d.knockRetard, 1, "", 4f, 6f);
        tile(c, t, l, 2, Icons.THROTTLE, "ДРОССЕЛЬ", d.throttle, 0, "%", Float.NaN, Float.NaN);
        tile(c, t, l, 3, Icons.SPEED, "СКОРОСТЬ", d.speed, 0, "км/ч", Float.NaN, Float.NaN);
    }

    private static void tile(Canvas c, Theme t, Layout l, int i, int icon, String label,
                             Channel ch, int decimals, String unit,
                             float warnFrom, float alertFrom) {
        OemTile.draw(c, t, l.tileX(i), l.tileYChassis, l.tileW, l.tileH,
                icon, label, ch, decimals, unit, warnFrom, alertFrom);
    }
}
