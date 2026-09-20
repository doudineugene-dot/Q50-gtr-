package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Карточка данных штатного вида: скруглённая панель со светлой кромкой,
 * подпись капителью сверху, ниже пиктограмма, значение и единица.
 *
 * Размеры — docs/UI-MASTER-SPEC.md, п.6. Панель и пиктограмма статичны,
 * число и его цвет — динамические.
 */
public final class OemTile {

    private OemTile() {
    }

    static void panel(Canvas c, Theme t, float x, float y, float w, float h) {
        float rr = 6f * (h / 92f);
        t.rect.set(x, y, x + w, y + h);
        // Заливка идёт градиентом сверху вниз: так на эталоне.
        c.drawRoundRect(t.rect, rr, rr,
                t.vertical3(Theme.TILE_TOP, Theme.TILE, Theme.TILE_BOTTOM, y, y + h));
        t.rect.set(x, y, x + w, y + h);
        c.drawRoundRect(t.rect, rr, rr, t.stroke(Theme.TILE_EDGE, 1.4f));
    }

    /** Рисует подпись, уменьшая кегль, пока она не влезет в maxW. */
    static void fit(Canvas c, Theme t, String label, float x, float baseline,
                    float maxW, float size) {
        Paint p = t.text(Theme.LABEL, size, Paint.Align.LEFT, false);
        for (int i = 0; i < 8 && p.measureText(label) > maxW; i++) {
            size -= 0.6f;
            p = t.text(Theme.LABEL, size, Paint.Align.LEFT, false);
        }
        c.drawText(label, x, baseline, p);
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

    /** Псевдоединица: коррекция топлива выводится со знаком, как на эталоне. */
    public static final String SIGNED = "\u00B1%";

    /** Значение крупно, единица мелко справа от него. */
    static void value(Canvas c, Theme t, float x, float baseline, Channel ch,
                      int decimals, String unit, int color, float size, float gap) {
        String s = ch.hasValue() ? ch.text(decimals) : "—";
        if (SIGNED.equals(unit)) {
            unit = "%";
            if (ch.hasValue() && ch.getValue() >= 0f) {
                s = "+" + s;
            }
        }
        Paint p = t.textFor(s, color, size, Paint.Align.LEFT, false);
        float vw = p.measureText(s);
        c.drawText(s, x, baseline, p);
        if (ch.hasValue() && unit != null && unit.length() > 0) {
            c.drawText(unit, x + vw + gap, baseline,
                    t.textFor(unit, Theme.LABEL, size * 0.45f, Paint.Align.LEFT, false));
        }
    }

    /** Значение, выключенное по центру: давления в стойках на ШАССИ. */
    public static void centred(Canvas c, Theme t, float cx, float baseline,
                               Channel ch, int decimals, float size) {
        String s = ch.hasValue() ? ch.text(decimals) : "—";
        c.drawText(s, cx, baseline,
                t.textFor(s, ch.hasValue() ? Theme.WHITE : Theme.VALUE_DIM, size,
                        Paint.Align.CENTER, false));
    }

    /** Карточка нижнего ряда. */
    public static void draw(Canvas c, Theme t, float x, float y, float w, float h,
                            int icon, String label, Channel ch, int decimals,
                            String unit, float warnFrom, float alertFrom) {
        float s = h / 92f;
        panel(c, t, x, y, w, h);
        // Замеры по эталону: подпись 319..332, значение 350..372,
        // пиктограмма x+15..x+38, число с x+51.
        // Android 2.3 не знает семейства sans-serif-condensed и молча
        // подставляет обычный Droid Sans, который шире. Поэтому подпись
        // ужимается под ширину карточки, а не обрезается по краю.
        fit(c, t, label, x + 12f * s, y + 28f * s, w - 20f * s, 13f * s);
        float iconSize = 24f * s;
        Icons.draw(c, t, icon, x + 14f * s, y + h - iconSize - 20f * s, iconSize, Theme.TICK);
        value(c, t, x + 50f * s, y + h - 20f * s, ch, decimals, unit,
                colorFor(ch, warnFrom, alertFrom), 30f * s, 8f * s);
    }

    /** Карточка правой колонки ШАССИ: пиктограмма в рамке слева. */
    public static void row(Canvas c, Theme t, float x, float y, float w, float h,
                           int icon, String label, Channel ch, int decimals,
                           String unit, float warnFrom, float alertFrom) {
        float s = h / 64f;
        panel(c, t, x, y, w, h);
        float box = h - 20f * s;
        t.rect.set(x + 9f * s, y + 10f * s, x + 9f * s + box, y + 10f * s + box);
        c.drawRoundRect(t.rect, 4f * s, 4f * s, t.stroke(Theme.TILE_EDGE, 1f * s));
        Icons.draw(c, t, icon, x + 9f * s + box * 0.18f, y + 10f * s + box * 0.18f,
                box * 0.64f, Theme.TICK);
        float tx = x + 9f * s + box + 12f * s;
        fit(c, t, label, tx, y + h * 0.40f, x + w - 8f * s - tx, 12f * s);
        value(c, t, tx, y + h * 0.88f, ch, decimals, unit,
                colorFor(ch, warnFrom, alertFrom), 25f * s, 14f * s);
    }
}
