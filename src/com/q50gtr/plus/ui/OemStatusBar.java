package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Верхняя полоса штатного InTouch: стрелка назад, часы, наружная температура,
 * уровень сигнала, Bluetooth и разделительная линия снизу.
 *
 * Позиции — docs/UI-MASTER-SPEC.md, п.4. Статичны только пиктограммы;
 * время, температура и признак источника данных приходят из DataHub.
 */
public final class OemStatusBar {

    private OemStatusBar() {
    }

    public static void draw(Canvas c, Theme t, Layout l,
                            String clock, String temp, String note) {
        c.drawRect(0f, 0f, l.w, Layout.TOP_H, t.fill(Theme.BG));

        float mid = Layout.TOP_H * 0.5f;

        // Стрелка «назад».
        chevron(c, t, l.backCx(), mid, 9f, 12f, false);

        // Часы на эталоне занимают строки 19..36, базовая линия 36.
        c.drawText(clock, l.clockCx(), 36f,
                t.text(Theme.WHITE, 22f, Paint.Align.CENTER, false));

        // Число и «°C» стоят рядом, а не друг на друге: на эталоне
        // блок «15 °C» занимает x 327..366, то есть 39 px.
        Paint tp = t.text(Theme.WHITE, 21f, Paint.Align.LEFT, false);
        float tw = tp.measureText(temp);
        float tx = l.tempCx() - (tw + 22f) * 0.5f;
        c.drawText(temp, tx, 36f, tp);
        c.drawText("°C", tx + tw + 6f, 34f,
                t.text(Theme.WHITE, 14f, Paint.Align.LEFT, false));

        signal(c, t, l.signalCx(), mid);
        bluetooth(c, t, l.bluetoothCx(), mid);

        if (note != null && note.length() > 0) {
            c.drawText(note, l.tempCx() + 58f, mid + 5f,
                    t.text(Theme.ACCENT, 10f, Paint.Align.LEFT, false));
        }

        // Разделитель: сине-лавандовый, ядро на строке 45, с затуханием.
        c.drawRect(0f, Layout.TOP_H, l.w, Layout.TOP_H + 0.9f, t.fill(0xFF4B4D63));
        c.drawRect(0f, Layout.TOP_H + 0.9f, l.w, Layout.TOP_H + 2.1f, t.fill(Theme.RULE));
        c.drawRect(0f, Layout.TOP_H + 2.1f, l.w, Layout.TOP_H + 3f, t.fill(0xFF2C2E44));
    }

    /** Шеврон, он же стрелка назад и стрелки листания вкладок. */
    static void chevron(Canvas c, Theme t, float cx, float cy,
                        float halfW, float halfH, boolean right) {
        float d = right ? 1f : -1f;
        Paint p = t.stroke(Theme.WHITE, 2.6f);
        // Остриё в направлении d, хвосты — назад.
        c.drawLine(cx - d * halfW * 0.5f, cy - halfH,
                cx + d * halfW * 0.5f, cy, p);
        c.drawLine(cx + d * halfW * 0.5f, cy,
                cx - d * halfW * 0.5f, cy + halfH, p);
    }

    private static void signal(Canvas c, Theme t, float cx, float cy) {
        float x = cx - 6f;
        for (int i = 0; i < 4; i++) {
            float h = 3f + i * 2.6f;
            c.drawRect(x + i * 3.4f, cy + 5f - h, x + i * 3.4f + 2.2f, cy + 5f,
                    t.fill(Theme.WHITE));
        }
    }

    private static void bluetooth(Canvas c, Theme t, float cx, float cy) {
        Paint p = t.stroke(0xFF2F8FE0, 2f);
        float h = 8f;
        c.drawLine(cx, cy - h, cx, cy + h, p);
        c.drawLine(cx, cy - h, cx + 5f, cy - h * 0.45f, p);
        c.drawLine(cx + 5f, cy - h * 0.45f, cx - 5f, cy + h * 0.45f, p);
        c.drawLine(cx - 5f, cy - h * 0.45f, cx + 5f, cy + h * 0.45f, p);
        c.drawLine(cx + 5f, cy + h * 0.45f, cx, cy + h, p);
    }
}
