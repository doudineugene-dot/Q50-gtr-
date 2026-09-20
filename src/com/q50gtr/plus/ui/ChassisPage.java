package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleData;

/** Экран 3 по docs/UI-MASTER-SPEC.md: пневмоподвеска, колонка справа, ряд снизу. */
public final class ChassisPage implements Page {

    private static final float CAR_X = 180f;
    private static final float CAR_W = 230f;
    private static final float CAR_H = 162f;

    public String title() {
        return "ШАССИ";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float carY = 50f;

        // Заголовок идёт первой строкой контента, под верхней полосой.
        c.drawText("ДАВЛЕНИЕ ПНЕВМОПОДВЕСКИ (bar)", CAR_X + CAR_W / 2f, 30f,
                t.text(Theme.LABEL, 13f, Paint.Align.CENTER, false));

        Q50Rear.car(c, t, CAR_X, carY, CAR_W, CAR_H);

        float strutH = 66f;
        float topCy = carY + 34f;
        float botCy = carY + 128f;
        Q50Rear.strut(c, t, 112f, topCy, strutH, d.airFrontLeft, true);
        Q50Rear.strut(c, t, 478f, topCy, strutH, d.airFrontRight, false);
        Q50Rear.strut(c, t, 112f, botCy, strutH, d.airRearLeft, true);
        Q50Rear.strut(c, t, 478f, botCy, strutH, d.airRearRight, false);

        float colX = 598f;
        float colW = 228f;
        float rh = 56f;
        float gap = 8f;
        float ry = 14f;
        Gauges.tileRow(c, t, colX, ry, colW, rh, Icons.GEARBOX, "ТЕМП. АКПП",
                d.transmissionTemp, 0, "°C", 110f, 125f);
        Gauges.tileRow(c, t, colX, ry + (rh + gap), colW, rh, Icons.GEARBOX, "ТЕМП. РАЗДАТКИ",
                d.transferCaseTemp, 0, "°C", 110f, 125f);
        Gauges.tileRow(c, t, colX, ry + 2f * (rh + gap), colW, rh, Icons.BATTERY, "НАПРЯЖЕНИЕ",
                d.batteryVoltage, 1, "V", Float.NaN, Float.NaN);
        Gauges.tileRow(c, t, colX, ry + 3f * (rh + gap), colW, rh, Icons.AMBIENT, "ТЕМП. ОКР. ВОЗДУХА",
                d.ambientTemp, 0, "°C", Float.NaN, Float.NaN);

        Channel knock = d.maxKnockIndexChannel();
        Layout.tile(c, t, 0, Icons.SPARK, "УГОЛ ЗАЖИГАНИЯ", d.ignitionTiming, 0, "°", Float.NaN, Float.NaN);
        Layout.tile(c, t, 1, Icons.KNOCK, "KNOCK INDEX (MAX)",
                knock != null ? knock : d.knockRetard, 1, "", 4f, 6f);
        Layout.tile(c, t, 2, Icons.THROTTLE, "ДРОССЕЛЬ", d.throttle, 0, "%", Float.NaN, Float.NaN);
        Layout.tile(c, t, 3, Icons.SPEED, "СКОРОСТЬ", d.speed, 0, "км/ч", Float.NaN, Float.NaN);
    }
}
