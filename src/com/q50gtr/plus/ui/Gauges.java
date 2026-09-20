package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Карточки нижнего ряда и правой колонки. Геометрия — из
 * docs/UI-MASTER-SPEC.md, раздел «Карточки».
 *
 * Круглые приборы вынесены в {@link OemDial}.
 */
public final class Gauges {

    private Gauges() {
    }

    /* ------------------------------------------------------------------ */
    /* плитки                                                              */
    /* ------------------------------------------------------------------ */

    private static void panel(Canvas c, Theme t, float x, float y, float w, float h) {
        int save = c.save();
        c.translate(x, y);
        t.rect2.set(0f, 0f, w, h);
        c.drawRoundRect(t.rect2, 4f, 4f, t.fill(Theme.TILE));
        c.restoreToCount(save);
        t.rect.set(x, y, x + w, y + h);
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(Theme.TILE_EDGE, 1f));
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
        c.drawText(label, x + 12f, y + 23f,
                t.text(Theme.LABEL, 12.5f, Paint.Align.LEFT, false));
        float iconSize = 26f;
        Icons.draw(c, t, icon, x + 11f, y + h - iconSize - 17f, iconSize, Theme.TICK);
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
