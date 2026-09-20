package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.q50gtr.plus.data.Channel;

/**
 * Динамическая часть круглого прибора: стрелка, ступица, название прибора,
 * единица и цифровое значение. Кольцо, циферблат, деления, цифры шкалы и
 * красная зона приходят с фонового растра — их рисовать не нужно.
 *
 * Углы шкалы измерены по MASTER (docs/UI-MASTER-SPEC.md, п.5): первое
 * деление 146.2°, последнее 393.7°. Угол стрелки считается строго как
 * (value − min) / (max − min) — декоративных сдвигов нет.
 */
public final class OemDial {

    public static final float ARC_START = 146.2f;
    public static final float ARC_SWEEP = 247.5f;

    /* Доли радиуса по замерам эталона: стрелка тонкая (3 px у ступицы,
     * 2 px у кончика), ступица — тёмный диск радиусом 18 px. */
    private static final float TIP = 0.780f;
    private static final float TAIL = 0.02f;
    private static final float HUB_OUT = 0.145f;

    private OemDial() {
    }

    /** Стрелка. Рисуется первой, чтобы ступица и подписи легли поверх. */
    public static void needle(Canvas c, Theme t, float cx, float cy, float r,
                              Channel ch, float min, float max, float redlineFrom) {
        if (!ch.hasValue()) {
            return;
        }
        float f = (ch.getValue() - min) / (max - min);
        f = f < 0f ? 0f : (f > 1f ? 1f : f);
        double a = Math.toRadians(ARC_START + ARC_SWEEP * f);
        float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
        float nx = -sin, ny = cos;
        boolean hot = !Float.isNaN(redlineFrom) && ch.getValue() >= redlineFrom;

        // Клин: у ступицы шире, к кончику сходится — как на эталоне.
        float base = r * 0.0125f;
        float tip = r * 0.0070f;
        Path p = t.path;
        p.reset();
        p.moveTo(cx + cos * r * TIP + nx * tip, cy + sin * r * TIP + ny * tip);
        p.lineTo(cx + cos * r * TIP - nx * tip, cy + sin * r * TIP - ny * tip);
        p.lineTo(cx - cos * r * TAIL - nx * base, cy - sin * r * TAIL - ny * base);
        p.lineTo(cx - cos * r * TAIL + nx * base, cy - sin * r * TAIL + ny * base);
        p.close();
        c.drawPath(p, t.fill(hot ? Theme.RED : Theme.NEEDLE));
    }

    /** Ступица: тёмный диск со светлым ободком, поверх стрелки. */
    public static void hub(Canvas c, Theme t, float cx, float cy, float r) {
        c.drawCircle(cx, cy, r * HUB_OUT, t.fill(Theme.HUB_RIM));
        c.drawCircle(cx, cy, r * (HUB_OUT - 0.012f), t.fill(Theme.HUB_FILL));
        c.drawCircle(cx, cy, r * 0.032f, t.fill(Theme.HUB_CORE));
    }

    /**
     * Название прибора и единица под ним. Базовые линии сняты с эталона:
     * подпись на cy−44, единица на cy−29 при cy = 181.
     */
    public static void caption(Canvas c, Theme t, float cx, float cy, float dy,
                               String name, String unit) {
        c.drawText(name, cx, cy - 44f + dy,
                t.text(Theme.WHITE, 17f, Paint.Align.CENTER, false));
        if (unit != null) {
            c.drawText(unit, cx, cy - 29f + dy,
                    t.text(Theme.LABEL, 11.5f, Paint.Align.CENTER, false));
        }
    }

    /**
     * Цифровое значение под ступицей (есть только на вкладке ТОПЛИВО).
     * Базовая линия 258, то есть cy + 77.
     */
    public static void value(Canvas c, Theme t, float cx, float cy,
                             Channel ch, int decimals, String emptyNote) {
        if (ch.hasValue()) {
            c.drawText(ch.text(decimals), cx + 3f, cy + 77f,
                    t.text(Theme.WHITE, 27f, Paint.Align.CENTER, false));
            return;
        }
        // Канал не логируется — прибор остаётся, но числа нет.
        c.drawText("—", cx + 3f, cy + 75f,
                t.text(Theme.VALUE_DIM, 26f, Paint.Align.CENTER, false));
        if (emptyNote != null) {
            c.drawText(emptyNote, cx + 3f, cy + 95f,
                    t.text(Theme.VALUE_DIM, 11f, Paint.Align.CENTER, false));
        }
    }

    /** Буква селектора передач в декоративном кружке под осью RPM. */
    public static void gear(Canvas c, Theme t, float cx, float cy, String gear) {
        c.drawText(gear, cx + 1f, cy + 80f,
                t.text(Theme.WHITE, 36f, Paint.Align.CENTER, false));
    }
}
