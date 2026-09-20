package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Круглый прибор штатного кластера Q50. Радиальная раскладка снята с master
 * reference измерением профиля яркости вдоль радиуса — см.
 * docs/UI-MASTER-SPEC.md, раздел «Приборы».
 *
 * Положение стрелки — строго (value − min) / (max − min). Никаких
 * декоративных сдвигов: шкала должна соответствовать числу.
 */
public final class OemDial {

    public static final float ARC_START = 150f;
    public static final float ARC_SWEEP = 240f;

    /* Доли радиуса — из профиля эталона. */
    private static final float BEZEL_IN   = 0.945f;
    private static final float RING       = 0.928f;
    private static final float RED_R      = 0.875f;
    private static final float TICK_OUT   = 0.865f;
    private static final float TICK_MAJ   = 0.735f;
    private static final float TICK_MIN   = 0.790f;
    private static final float LABEL_R    = 0.615f;

    private OemDial() {
    }

    public static void draw(Canvas c, Theme t, float cx, float cy, float r,
                            Channel ch, float min, float max, int decimals,
                            int majorTicks, float redlineFrom,
                            String caption, String unit,
                            float tickScale, int tickDecimals,
                            boolean showValue, String emptyNote) {

        int save = c.save();
        c.translate(cx, cy);

        // 1. Обод, тёмное кольцо, циферблат.
        c.drawCircle(0f, 0f, r, t.bezel(r));
        c.drawCircle(0f, 0f, r * BEZEL_IN, t.fill(Theme.RING_DARK));
        c.drawCircle(0f, 0f, r * RING, t.dial(r));

        // 2. Красная зона — тонкая дуга поверх циферблата.
        if (!Float.isNaN(redlineFrom) && redlineFrom > min && redlineFrom < max) {
            // На мастере это широкий сектор на всю ширину кольца шкалы,
            // а не тонкая дуга.
            float band = (TICK_OUT - TICK_MAJ) * r;
            float rr = (TICK_OUT + TICK_MAJ) / 2f * r;
            t.rect.set(-rr, -rr, rr, rr);
            float f = (redlineFrom - min) / (max - min);
            float from = ARC_START + ARC_SWEEP * f;
            c.drawArc(t.rect, from, ARC_START + ARC_SWEEP - from, false,
                    t.stroke(Theme.RED, band));
        }

        // 3. Промежуточные риски.
        int minorCount = majorTicks * 2;
        for (int i = 0; i <= minorCount; i++) {
            if (i % 2 == 0) {
                continue;
            }
            float f = (float) i / minorCount;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * r * TICK_MIN, sin * r * TICK_MIN,
                    cos * r * TICK_OUT, sin * r * TICK_OUT,
                    t.stroke(Theme.TICK_MINOR, r * 0.024f));
        }

        // 4. Основные риски и подписи шкалы.
        Paint tickText = t.text(Theme.TICK, r * 0.175f, Paint.Align.CENTER, false);
        for (int i = 0; i <= majorTicks; i++) {
            float f = (float) i / majorTicks;
            float value = min + (max - min) * f;
            boolean hot = !Float.isNaN(redlineFrom) && value >= redlineFrom - 1e-4f;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * r * TICK_MAJ, sin * r * TICK_MAJ,
                    cos * r * TICK_OUT, sin * r * TICK_OUT,
                    t.stroke(hot ? Theme.RED : Theme.TICK, r * 0.036f));
            tickText.setColor(hot ? Theme.RED : Theme.TICK);
            c.drawText(Channel.format(value / tickScale, tickDecimals),
                    cos * r * LABEL_R, sin * r * LABEL_R + r * 0.061f, tickText);
        }

        boolean has = ch.hasValue();

        // 5. Стрелка и ступица — до подписей, иначе перечёркивает caption.
        if (has) {
            float f = (ch.getValue() - min) / (max - min);
            f = f < 0f ? 0f : (f > 1f ? 1f : f);
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            boolean hot = !Float.isNaN(redlineFrom) && ch.getValue() >= redlineFrom;
            float nx = -sin, ny = cos;              // нормаль к стрелке
            float baseW = r * 0.030f;
            android.graphics.Path np = t.path;
            np.reset();
            np.moveTo(cos * r * 0.760f, sin * r * 0.760f);
            np.lineTo(-cos * r * 0.17f + nx * baseW, -sin * r * 0.17f + ny * baseW);
            np.lineTo(-cos * r * 0.17f - nx * baseW, -sin * r * 0.17f - ny * baseW);
            np.close();
            c.drawPath(np, t.fill(hot ? Theme.RED : 0xFFFFFFFF));
        }
        c.drawCircle(0f, 0f, r * 0.115f, t.fill(0xFF878F9C));
        c.drawCircle(0f, 0f, r * 0.092f, t.fill(0xFF2A313B));
        c.drawCircle(0f, 0f, r * 0.040f, t.fill(0xFF0E131B));

        // 6. Название, единица, цифровое значение.
        c.drawText(caption, 0f, -r * 0.30f,
                t.text(Theme.WHITE, r * 0.125f, Paint.Align.CENTER, false));
        if (unit != null) {
            c.drawText(unit, 0f, -r * 0.175f,
                    t.text(Theme.LABEL, r * 0.088f, Paint.Align.CENTER, false));
        }
        if (showValue && has) {
            c.drawText(ch.text(decimals), 0f, r * 0.44f,
                    t.text(Theme.WHITE, r * 0.235f, Paint.Align.CENTER, false));
        } else if (!has && emptyNote != null) {
            // Канал не логируется — говорим об этом, а не рисуем число.
            c.drawText("—", 0f, r * 0.42f,
                    t.text(Theme.VALUE_DIM, r * 0.24f, Paint.Align.CENTER, false));
            c.drawText(emptyNote, 0f, r * 0.62f,
                    t.text(Theme.VALUE_DIM, r * 0.095f, Paint.Align.CENTER, false));
        }

        c.restoreToCount(save);
    }

    /** Круглый индикатор селектора передач в нижней части циферблата. */
    public static void gearBadge(Canvas c, Theme t, float cx, float cy, float r, String gear) {
        float bx = cx;
        float by = cy + r * 0.52f;
        float br = r * 0.155f;
        c.drawCircle(bx, by, br, t.fill(0xFF0A0F18));
        c.drawCircle(bx, by, br, t.stroke(Theme.LABEL, 1.4f));
        c.drawText(gear, bx, by + br * 0.42f,
                t.text(Theme.WHITE, br * 1.15f, Paint.Align.CENTER, false));
    }
}
