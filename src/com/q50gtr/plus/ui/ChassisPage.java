package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleData;

/**
 * Экран 3 по docs/UI-MASTER-SPEC.md, п.8: слева Q50 сзади с четырьмя стойками
 * пневмоподвески, справа колонка из четырёх карточек, внизу общий ряд карточек.
 */
public final class ChassisPage implements Page {

    public String title() {
        return "ШАССИ";
    }

    public void draw(Canvas c, Theme t, Layout l, VehicleData d) {
        float leftW = l.chassisSplitX();

        // Все числа ниже — из docs/UI-MASTER-SPEC.md, п.8, в координатах
        // рамки эталона; по горизонтали они тянутся вместе с левой половиной.
        float k = leftW / 345f;

        c.drawText("ДАВЛЕНИЕ ПНЕВМОПОДВЕСКИ (bar)", leftW * 0.52f, 78f - Layout.TOP_H,
                t.text(Theme.LABEL, 14f, Paint.Align.CENTER, false));

        float carX = 69f * k;
        float carW = 198f * k;
        float carY = 125f - Layout.TOP_H;
        if (Sprites.hasCar()) {
            Sprites.car(c, t, carX, carY, carW, 138f);
        } else {
            Q50Rear.car(c, t, carX, carY, carW, 138f);
        }

        // Стойки: x 31..57 и 283..309, растр 27x119 на y 129..248.
        float strutL = 31f * k;
        float strutR = 283f * k;
        float strutW = 27f * k;
        float sy = 129f - Layout.TOP_H;
        if (Sprites.hasStrut()) {
            Sprites.strut(c, t, strutL, sy, strutW, 119f);
            Sprites.strut(c, t, strutR, sy, strutW, 119f);
        } else {
            Q50Rear.strut(c, t, strutL + strutW / 2f, sy + 30f, 59f, d.airFrontLeft, true);
            Q50Rear.strut(c, t, strutR + strutW / 2f, sy + 30f, 59f, d.airFrontRight, true);
            Q50Rear.strut(c, t, strutL + strutW / 2f, sy + 89f, 59f, d.airRearLeft, false);
            Q50Rear.strut(c, t, strutR + strutW / 2f, sy + 89f, 59f, d.airRearRight, false);
        }
        // Давления — динамические, поверх статичного растра.
        pressure(c, t, strutL + strutW / 2f, sy - 9f, d.airFrontLeft);
        pressure(c, t, strutR + strutW / 2f, sy - 9f, d.airFrontRight);
        pressure(c, t, strutL + strutW / 2f, sy + 119f + 24f, d.airRearLeft);
        pressure(c, t, strutR + strutW / 2f, sy + 119f + 24f, d.airRearRight);

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

    private static void pressure(Canvas c, Theme t, float cx, float baseline, Channel ch) {
        c.drawText(ch.hasValue() ? ch.text(1) : "—", cx, baseline,
                t.text(ch.hasValue() ? Theme.WHITE : Theme.VALUE_DIM, 23f,
                        Paint.Align.CENTER, false));
    }

    private static void tile(Canvas c, Theme t, Layout l, int i, int icon, String label,
                             Channel ch, int decimals, String unit,
                             float warnFrom, float alertFrom) {
        OemTile.draw(c, t, l.tileX(i), Layout.TILE_Y_CHASSIS, l.tileW, Layout.TILE_H,
                icon, label, ch, decimals, unit, warnFrom, alertFrom);
    }
}
