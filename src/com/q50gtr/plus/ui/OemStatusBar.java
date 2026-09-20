package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Верхняя полоса: стрелка «назад», часы, наружная температура, уровень
 * сигнала, Bluetooth и разделитель под полосой.
 *
 * Всё рисуется Canvas в конечном размере — ни одного растра, поэтому кромки
 * остаются резкими при любой геометрии окна. Пропорции сняты с эталона
 * (docs/UI-MASTER-SPEC.md, п.4) и умножаются на масштаб окна.
 */
public final class OemStatusBar {

    private OemStatusBar() {
    }

    public static void draw(Canvas c, Theme t, Layout l,
                            String clock, String temp, String note) {
        float s = l.s;
        c.drawRect(0f, 0f, l.w, l.topH, t.fill(Theme.BG));

        float mid = l.topH * 0.5f;
        float base = 36f * s;

        chevron(c, t, l.backCx(), mid, 9f * s, 12f * s, false);

        c.drawText(clock, l.clockCx(), base,
                t.text(Theme.WHITE, l.text(22f), Paint.Align.CENTER, false));

        // Значение и «°C» стоят рядом; блок целиком центрируется.
        Paint vp = t.text(Theme.WHITE, l.text(23.5f), Paint.Align.LEFT, false);
        float vw = vp.measureText(temp);
        Paint up = t.text(Theme.WHITE, l.text(14f), Paint.Align.LEFT, false);
        float uw = up.measureText("°C");
        float x = l.tempCx() - (vw + 5f * s + uw) * 0.5f;
        c.drawText(temp, x, base, vp);
        c.drawText("°C", x + vw + 5f * s, base - 1f * s,
                t.text(Theme.WHITE, l.text(14f), Paint.Align.LEFT, false));

        signal(c, t, l.signalCx(), mid, s);
        bluetooth(c, t, l.bluetoothCx(), mid, s);

        if (note != null && note.length() > 0) {
            // Единственная отметка демо-режима на всей панели.
            c.drawText(note, l.demoCx(), base - 2f * s,
                    t.text(Theme.ACCENT, l.text(12f), Paint.Align.CENTER, true));
        }

        // Разделитель под полосой: сине-лавандовый, с затуханием.
        c.drawRect(0f, l.topH, l.w, l.topH + 0.9f * s, t.fill(0xFF4B4D63));
        c.drawRect(0f, l.topH + 0.9f * s, l.w, l.topH + 2.1f * s, t.fill(Theme.RULE));
        c.drawRect(0f, l.topH + 2.1f * s, l.w, l.topH + 3f * s, t.fill(0xFF2C2E44));
    }

    /** Шеврон: стрелка «назад» и стрелки листания вкладок. */
    static void chevron(Canvas c, Theme t, float cx, float cy,
                        float halfW, float halfH, boolean right) {
        float d = right ? 1f : -1f;
        Paint p = t.stroke(Theme.WHITE, Math.max(1.6f, halfH * 0.22f));
        c.drawLine(cx - d * halfW * 0.5f, cy - halfH, cx + d * halfW * 0.5f, cy, p);
        c.drawLine(cx + d * halfW * 0.5f, cy, cx - d * halfW * 0.5f, cy + halfH, p);
    }

    private static void signal(Canvas c, Theme t, float cx, float cy, float s) {
        float x = cx - 6f * s;
        for (int i = 0; i < 4; i++) {
            float bh = (3f + i * 2.6f) * s;
            c.drawRect(x + i * 3.4f * s, cy + 5f * s - bh,
                    x + i * 3.4f * s + 2.2f * s, cy + 5f * s, t.fill(Theme.WHITE));
        }
    }

    private static void bluetooth(Canvas c, Theme t, float cx, float cy, float s) {
        Paint p = t.stroke(0xFF2F8FE0, 2f * s);
        float hh = 8f * s;
        float hw = 5f * s;
        c.drawLine(cx, cy - hh, cx, cy + hh, p);
        c.drawLine(cx, cy - hh, cx + hw, cy - hh * 0.45f, p);
        c.drawLine(cx + hw, cy - hh * 0.45f, cx - hw, cy + hh * 0.45f, p);
        c.drawLine(cx - hw, cy - hh * 0.45f, cx + hw, cy + hh * 0.45f, p);
        c.drawLine(cx + hw, cy + hh * 0.45f, cx, cy + hh, p);
    }
}
