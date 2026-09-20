package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Динамическая часть карточки: только само значение и единица рядом с ним.
 * Рамка, подпись и пиктограмма приходят с фонового растра.
 *
 * Метрики с эталона: цифры занимают строки 350..372, значение начинается на
 * x = карточка + 59, единица — через 8 px после числа.
 */
public final class OemTile {

    private OemTile() {
    }

    static int colorFor(Channel ch, float warnFrom, float alertFrom) {
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

    /** Значение с единицей, выключка влево от x, по базовой линии baseline. */
    public static void value(Canvas c, Theme t, float x, float baseline,
                             Channel ch, int decimals, String unit,
                             float warnFrom, float alertFrom, float size, float gap) {
        String s = ch.hasValue() ? ch.text(decimals) : "—";
        if (SIGNED.equals(unit)) {
            unit = "%";
            if (ch.hasValue() && ch.getValue() >= 0f) {
                s = "+" + s;
            }
        }
        Paint p = t.text(colorFor(ch, warnFrom, alertFrom), size,
                Paint.Align.LEFT, false);
        c.drawText(s, x, baseline, p);
        if (ch.hasValue() && unit != null && unit.length() > 0) {
            float w = p.measureText(s);
            c.drawText(unit, x + w + gap,
                    baseline, t.text(Theme.LABEL, size * 0.47f, Paint.Align.LEFT, false));
        }
    }

    /** Псевдоединица: коррекция топлива выводится со знаком, как на эталоне. */
    public static final String SIGNED = "\u00B1%";

    /** Значение, выключенное по центру (давления в стойках на ШАССИ). */
    public static void centred(Canvas c, Theme t, float cx, float baseline,
                               Channel ch, int decimals, float size) {
        c.drawText(ch.hasValue() ? ch.text(decimals) : "—", cx, baseline,
                t.text(ch.hasValue() ? Theme.WHITE : Theme.VALUE_DIM, size,
                        Paint.Align.CENTER, false));
    }
}
