package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Динамическая часть верхней полосы: часы, наружная температура и отметка
 * демо-режима. Стрелка «назад», шкала сигнала и Bluetooth статичны и лежат
 * на фоновом растре.
 *
 * Метрики с эталона: обе надписи стоят на базовой линии 31; часы
 * центрированы по x = 230.5 рамки, блок «15 °C» — по 346.5.
 */
public final class OemStatusBar {

    private OemStatusBar() {
    }

    public static void draw(Canvas c, Theme t, Layout l,
                            String clock, String temp, boolean demo) {
        c.drawText(clock, l.x(Layout.CLOCK_CX), Layout.CLOCK_BASE,
                t.text(Theme.WHITE, 22.5f, Paint.Align.CENTER, false));

        // Значение и «°C» стоят рядом, блок целиком центрируется.
        Paint vp = t.text(Theme.WHITE, 23.5f, Paint.Align.LEFT, false);
        float vw = vp.measureText(temp);
        Paint up = t.text(Theme.WHITE, 14f, Paint.Align.LEFT, false);
        float uw = up.measureText("°C");
        float x = l.x(Layout.TEMP_CX) - (vw + 5f + uw) * 0.5f;
        c.drawText(temp, x, Layout.CLOCK_BASE, vp);
        c.drawText("°C", x + vw + 5f, Layout.CLOCK_BASE - 1f,
                t.text(Theme.WHITE, 14f, Paint.Align.LEFT, false));

        if (demo) {
            // Единственная отметка демо-режима: на приборах её нет, там она
            // только мешает читать показания.
            c.drawText("DEMO", l.x(Layout.DEMO_CX), Layout.CLOCK_BASE - 2f,
                    t.text(Theme.ACCENT, 12f, Paint.Align.CENTER, true));
        }
    }
}
