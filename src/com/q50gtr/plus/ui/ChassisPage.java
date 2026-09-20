package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleData;

/**
 * Вкладка ШАССИ. Фон — chassis_bg_clean.png: автомобиль, стойки, рамки и
 * подписи карточек уже на нём. Здесь только числа — давления в стойках,
 * правая колонка и нижний ряд.
 */
public final class ChassisPage implements Page {

    public String title() {
        return "ШАССИ";
    }

    public void draw(Canvas c, Theme t, Layout l, VehicleData d) {
        OemTile.centred(c, t, l.strutCx(0), Layout.PRESS_BASE_FRONT, d.airFrontLeft, 1, 21f);
        OemTile.centred(c, t, l.strutCx(1), Layout.PRESS_BASE_FRONT, d.airFrontRight, 1, 21f);
        OemTile.centred(c, t, l.strutCx(0), Layout.PRESS_BASE_REAR, d.airRearLeft, 1, 21f);
        OemTile.centred(c, t, l.strutCx(1), Layout.PRESS_BASE_REAR, d.airRearRight, 1, 21f);

        col(c, t, l, 0, d.transmissionTemp, 0, "°C", 110f, 125f);
        col(c, t, l, 1, d.transferCaseTemp, 0, "°C", 110f, 125f);
        col(c, t, l, 2, d.batteryVoltage, 1, "V", Float.NaN, Float.NaN);
        col(c, t, l, 3, d.ambientTemp, 0, "°C", Float.NaN, Float.NaN);

        Channel knock = d.maxKnockIndexChannel();
        tile(c, t, l, 0, d.ignitionTiming, 0, "°", Float.NaN, Float.NaN);
        tile(c, t, l, 1, knock != null ? knock : d.knockRetard, 1, "", 4f, 6f);
        tile(c, t, l, 2, d.throttle, 0, "%", Float.NaN, Float.NaN);
        tile(c, t, l, 3, d.speed, 0, "км/ч", Float.NaN, Float.NaN);
    }

    private static void col(Canvas c, Theme t, Layout l, int i, Channel ch,
                            int decimals, String unit, float warnFrom, float alertFrom) {
        OemTile.value(c, t, l.x(Layout.COL_VALUE_X), l.colValueBase(i),
                ch, decimals, unit, warnFrom, alertFrom, 27f, 14f);
    }

    private static void tile(Canvas c, Theme t, Layout l, int i, Channel ch,
                             int decimals, String unit, float warnFrom, float alertFrom) {
        OemTile.value(c, t, l.tileValueX(i), Layout.TILE_VALUE_BASE_CHASSIS,
                ch, decimals, unit, warnFrom, alertFrom, 30f, 8f);
    }
}
