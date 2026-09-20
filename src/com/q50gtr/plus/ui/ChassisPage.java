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

        // Автомобиль и стойки кладутся в своём соотношении сторон.
        // Раньше ширина считалась через k, а высота через s — на 800x480 это
        // растягивало автомобиль по горизонтали почти в полтора раза.
        float strutH = 119f * s;
        float strutW = Sprites.hasStrut()
                ? Sprites.strutWidthFor(strutH) : 27f * s;
        float strutTop = 129f * s;

        // Стойки по краям левой половины, автомобиль — между ними.
        float strutL = 44f * k;
        float strutR = leftW - 49f * k;
        float gapL = strutL + strutW * 0.5f + 14f * s;
        float gapR = strutR - strutW * 0.5f - 14f * s;

        float carW = gapR - gapL;
        float carH = Sprites.hasCar()
                ? Sprites.carHeightFor(carW) : carW * 138f / 198f;
        float maxCarH = 150f * s;
        if (carH > maxCarH) {
            carH = maxCarH;
            carW = Sprites.hasCar()
                    ? carH * 198f / 138f : carH * 198f / 138f;
        }
        float carX = (gapL + gapR - carW) * 0.5f;
        float carY = strutTop + (strutH - carH) * 0.5f;

        if (Sprites.hasCar()) {
            Sprites.car(c, carX, carY, carW);
        } else {
            Q50Rear.car(c, t, carX, carY, carW, carH);
        }

        if (Sprites.hasStrut()) {
            Sprites.strut(c, strutL, strutTop, strutH);
            Sprites.strut(c, strutR, strutTop, strutH);
        } else {
            Q50Rear.strut(c, t, strutL, strutTop + strutH * 0.25f, strutH * 0.5f,
                    d.airFrontLeft, true);
            Q50Rear.strut(c, t, strutR, strutTop + strutH * 0.25f, strutH * 0.5f,
                    d.airFrontRight, true);
            Q50Rear.strut(c, t, strutL, strutTop + strutH * 0.75f, strutH * 0.5f,
                    d.airRearLeft, false);
            Q50Rear.strut(c, t, strutR, strutTop + strutH * 0.75f, strutH * 0.5f,
                    d.airRearRight, false);
        }

        // Давления — динамические, над и под стойками.
        float press = 21f * s;
        OemTile.centred(c, t, strutL, strutTop - 9f * s, d.airFrontLeft, 1, press);
        OemTile.centred(c, t, strutR, strutTop - 9f * s, d.airFrontRight, 1, press);
        OemTile.centred(c, t, strutL, strutTop + strutH + 26f * s, d.airRearLeft, 1, press);
        OemTile.centred(c, t, strutR, strutTop + strutH + 26f * s, d.airRearRight, 1, press);

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
