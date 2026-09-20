package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * Одна вкладка. Страница только читает VehicleData и рисует — она не знает,
 * откуда пришли числа.
 */
public interface Page {

    /** Подпись вкладки. */
    String title();

    /** Рисует вкладку в координатах окна; начало координат уже в контенте. */
    void draw(Canvas c, Theme t, Layout l, VehicleData d);
}
