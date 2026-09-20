package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Приборы штатного кластера Q50: металлический обод, тёмный циферблат,
 * длинные белые риски с лавандовыми промежуточными, красная зона у самого
 * обода и тонкая стрелка на серебристой оси.
 */
public final class Gauges {

    private static final float ARC_START = 150f;
    private static final float ARC_SWEEP = 240f;

    private Gauges() {
    }

    /* ------------------------------------------------------------------ */
    /* круглый прибор                                                      */
    /* ------------------------------------------------------------------ */

    public static void dial(Canvas c, Theme t, float cx, float cy, float r,
                            Channel ch, float min, float max, int decimals,
                            int majorTicks, float redlineFrom,
                            String caption, String subCaption, float tickScale,
                            int tickDecimals, boolean showValue) {

        int save = c.save();
        c.translate(cx, cy);

        // Обод, фаска, циферблат.
        c.drawCircle(0f, 0f, r, t.bezel(r));
        c.drawCircle(0f, 0f, r * 0.945f, t.fill(0xFF1B222C));
        c.drawCircle(0f, 0f, r * 0.915f, t.dial(r));

        // Красная зона — узкой дугой у самого обода, поверх кольца шкалы.
        float redR = r * 0.865f;
        if (!Float.isNaN(redlineFrom) && redlineFrom > min && redlineFrom < max) {
            t.rect.set(-redR, -redR, redR, redR);
            float f = (redlineFrom - min) / (max - min);
            float from = ARC_START + ARC_SWEEP * f;
            c.drawArc(t.rect, from, ARC_START + ARC_SWEEP - from, false,
                    t.stroke(Theme.RED, r * 0.045f));
        }

        float tickOuter = r * 0.835f;

        // Промежуточные риски — лавандовые, как в кластере.
        int minor = majorTicks * 2;
        for (int i = 0; i <= minor; i++) {
            if (i % 2 == 0) {
                continue;
            }
            float f = (float) i / minor;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * (tickOuter - r * 0.06f), sin * (tickOuter - r * 0.06f),
                    cos * tickOuter, sin * tickOuter, t.stroke(Theme.TICK_MINOR, r * 0.022f));
        }

        // Основные риски и цифры шкалы.
        Paint tickText = t.text(Theme.TICK, r * 0.135f, Paint.Align.CENTER, false);
        for (int i = 0; i <= majorTicks; i++) {
            float f = (float) i / majorTicks;
            float value = min + (max - min) * f;
            boolean hot = !Float.isNaN(redlineFrom) && value >= redlineFrom - 1e-4f;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * (tickOuter - r * 0.105f), sin * (tickOuter - r * 0.105f),
                    cos * tickOuter, sin * tickOuter,
                    t.stroke(hot ? Theme.RED : Theme.TICK, r * 0.028f));
            float lr = r * 0.625f;
            tickText.setColor(hot ? Theme.RED : Theme.TICK);
            c.drawText(Channel.format(value / tickScale, tickDecimals),
                    cos * lr, sin * lr + r * 0.048f, tickText);
        }

        boolean has = ch.hasValue();
        float f = 0f;
        if (has) {
            f = (ch.getValue() - min) / (max - min);
            f = f < 0f ? 0f : (f > 1f ? 1f : f);
        }

        // Стрелка, ось, и только потом подписи: иначе стрелка перечёркивает
        // название прибора, когда указывает вверх.
        if (has) {
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(-cos * r * 0.10f, -sin * r * 0.10f,
                    cos * r * 0.70f, sin * r * 0.70f,
                    t.stroke(Theme.WHITE, r * 0.024f));
        }
        c.drawCircle(0f, 0f, r * 0.10f, t.fill(0xFF9AA3B0));
        c.drawCircle(0f, 0f, r * 0.075f, t.fill(0xFF2B333F));
        c.drawCircle(0f, 0f, r * 0.03f, t.fill(0xFF11161E));

        c.drawText(caption, 0f, -r * 0.32f,
                t.text(Theme.WHITE, r * 0.135f, Paint.Align.CENTER, false));
        if (subCaption != null) {
            c.drawText(subCaption, 0f, -r * 0.185f,
                    t.text(Theme.LABEL, r * 0.092f, Paint.Align.CENTER, false));
        }
        if (showValue) {
            c.drawText(has ? ch.text(decimals) : "—", 0f, r * 0.46f,
                    t.text(has ? Theme.WHITE : Theme.VALUE_DIM, r * 0.26f,
                            Paint.Align.CENTER, false));
        }

        c.restoreToCount(save);
    }

    /* ------------------------------------------------------------------ */
    /* плитки                                                              */
    /* ------------------------------------------------------------------ */

    private static void panel(Canvas c, Theme t, float x, float y, float w, float h) {
        int save = c.save();
        c.translate(x, y);
        t.rect2.set(0f, 0f, w, h);
        c.drawRoundRect(t.rect2, 6f, 6f, t.tile(h));
        c.restoreToCount(save);
        t.rect.set(x, y, x + w, y + h);
        c.drawRoundRect(t.rect, 6f, 6f, t.stroke(Theme.TILE_EDGE, 1f));
    }

    private static int colorFor(Channel ch, float warnFrom, float alertFrom) {
        if (!ch.hasValue()) {
            return Theme.VALUE_DIM;
        }
        float v = ch.getValue();
        if (!Float.isNaN(alertFrom) && v >= alertFrom) {
            return Theme.RED;
        }
        if (!Float.isNaN(warnFrom) && v >= warnFrom) {
            return Theme.WARN;
        }
        return Theme.WHITE;
    }

    private static void value(Canvas c, Theme t, float x, float baseline,
                              Channel ch, int decimals, String unit, int color, float size) {
        String s = ch.hasValue() ? ch.text(decimals) : "—";
        Paint p = t.text(color, size, Paint.Align.LEFT, false);
        float vw = p.measureText(s);
        c.drawText(s, x, baseline, p);
        if (ch.hasValue() && unit != null && unit.length() > 0) {
            c.drawText(unit, x + vw + size * 0.14f, baseline,
                    t.text(Theme.LABEL, size * 0.44f, Paint.Align.LEFT, false));
        }
    }

    /** Плитка нижнего ряда: подпись сверху, ниже иконка и значение. */
    public static void tile(Canvas c, Theme t, float x, float y, float w, float h,
                            int icon, String label, Channel ch, int decimals,
                            String unit, float warnFrom, float alertFrom) {
        panel(c, t, x, y, w, h);
        c.drawText(label, x + 12f, y + 22f,
                t.text(Theme.LABEL, 12.5f, Paint.Align.LEFT, false));
        float iconSize = 26f;
        Icons.draw(c, t, icon, x + 11f, y + h - iconSize - 16f, iconSize, Theme.TICK);
        value(c, t, x + 11f + iconSize + 10f, y + h - 18f, ch, decimals, unit,
                colorFor(ch, warnFrom, alertFrom), 30f);
    }

    /** Плитка колонки: иконка в рамке слева, справа подпись и значение. */
    public static void tileRow(Canvas c, Theme t, float x, float y, float w, float h,
                               int icon, String label, Channel ch, int decimals,
                               String unit, float warnFrom, float alertFrom) {
        panel(c, t, x, y, w, h);
        float box = h - 18f;
        t.rect.set(x + 9f, y + 9f, x + 9f + box, y + 9f + box);
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(Theme.TILE_EDGE, 1f));
        Icons.draw(c, t, icon, x + 9f + box * 0.18f, y + 9f + box * 0.18f,
                box * 0.64f, Theme.TICK);
        float tx = x + 9f + box + 12f;
        c.drawText(label, tx, y + h * 0.42f,
                t.text(Theme.LABEL, 11.5f, Paint.Align.LEFT, false));
        value(c, t, tx, y + h * 0.86f, ch, decimals, unit,
                colorFor(ch, warnFrom, alertFrom), 24f);
    }
}
