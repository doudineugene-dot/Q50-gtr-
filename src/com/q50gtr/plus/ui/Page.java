package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * Динамический слой одной вкладки. Страница только читает VehicleData и
 * рисует поверх фонового растра — она не знает, откуда пришли числа.
 */
public interface Page {

    /** Подпись вкладки (используется для попадания пальцем и логов). */
    String title();

    /** Рисует динамику в координатах эталона; фон уже лежит под ней. */
    void draw(Canvas c, Theme t, Layout l, VehicleData d);
}
