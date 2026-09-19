package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.q50gtr.plus.data.Channel;

/**
 * The drawing vocabulary of the dashboard: panels, value tiles and arc gauges.
 *
 * All of it is plain Canvas work in the 840x480 design space, so it costs a
 * head unit almost nothing and looks like an instrument cluster rather than an
 * Android app.
 */
public final class Gauges {

    /** Arc gauges sweep from lower-left to lower-right, cluster style. */
    private static final float ARC_START = 150f;
    private static final float ARC_SWEEP = 240f;

    private static final String NO_DATA = "НЕТ ДАННЫХ";

    private Gauges() {
    }

    /* ------------------------------------------------------------------ */
    /* panels                                                              */
    /* ------------------------------------------------------------------ */

    public static void panel(Canvas c, Theme t, float x, float y, float w, float h) {
        t.rect.set(x, y, x + w, y + h);
        c.drawRoundRect(t.rect, 3f, 3f, t.fill(Theme.PANEL));
        c.drawRoundRect(t.rect, 3f, 3f, t.stroke(Theme.EDGE, 1f));
        // A single accent hairline along the top edge: the only "brand" mark.
        c.drawLine(x + 8f, y + 0.5f, x + w - 8f, y + 0.5f, t.stroke(Theme.ACCENT_DEEP, 1f));
    }

    /* ------------------------------------------------------------------ */
    /* value tiles                                                         */
    /* ------------------------------------------------------------------ */

    public static void tile(Canvas c, Theme t, float x, float y, float w, float h,
                            Channel ch, int decimals, float min, float max,
                            float warnFrom, float alertFrom) {
        tile(c, t, x, y, w, h, ch, decimals, min, max, warnFrom, alertFrom, false, null);
    }

    public static void tile(Canvas c, Theme t, float x, float y, float w, float h,
                            Channel ch, int decimals, float min, float max,
                            float warnFrom, float alertFrom, boolean bipolar, String labelOverride) {
        panel(c, t, x, y, w, h);

        String label = labelOverride != null ? labelOverride : ch.label;
        c.drawText(label, x + 10f, y + 19f, t.text(Theme.SILVER, 12f, Paint.Align.LEFT, false));

        if (ch.isDemo()) {
            demoBadge(c, t, x + w - 10f, y + 19f);
        }

        float valueSize = Math.min(40f, h * 0.40f);
        float baseline = y + h * 0.66f;

        if (!ch.hasValue()) {
            c.drawText(NO_DATA, x + w / 2f, baseline - valueSize * 0.25f,
                    t.text(Theme.DIM, 15f, Paint.Align.CENTER, false));
            return;
        }

        int color = colorFor(ch.getValue(), warnFrom, alertFrom);
        String value = ch.text(decimals);

        Paint vp = t.text(color, valueSize, Paint.Align.CENTER, true);
        float valueWidth = vp.measureText(value);
        float unitWidth = 0f;
        if (ch.unit.length() > 0) {
            t.text.setTextSize(13f);
            unitWidth = t.text.measureText(" " + ch.unit);
            t.text.setTextSize(valueSize);
        }
        float centre = x + w / 2f;
        float valueCentre = centre - unitWidth / 2f;

        c.drawText(value, valueCentre, baseline, t.text(color, valueSize, Paint.Align.CENTER, true));
        if (ch.unit.length() > 0) {
            c.drawText(ch.unit, valueCentre + valueWidth / 2f + 4f, baseline,
                    t.text(Theme.DIM, 13f, Paint.Align.LEFT, false));
        }

        if (max > min) {
            bar(c, t, x + 10f, y + h - 12f, w - 20f, 3f, ch.getValue(), min, max, color, bipolar);
        }
    }

    private static void bar(Canvas c, Theme t, float x, float y, float w, float h,
                            float value, float min, float max, int color, boolean bipolar) {
        t.rect.set(x, y, x + w, y + h);
        c.drawRect(t.rect, t.fill(Theme.EDGE_SOFT));

        float f = (value - min) / (max - min);
        f = f < 0f ? 0f : (f > 1f ? 1f : f);

        if (bipolar) {
            float mid = x + w / 2f;
            float end = x + w * f;
            t.rect.set(Math.min(mid, end), y, Math.max(mid, end), y + h);
            c.drawRect(t.rect, t.fill(color));
            c.drawLine(mid, y - 2f, mid, y + h + 2f, t.stroke(Theme.SILVER, 1f));
        } else {
            t.rect.set(x, y, x + w * f, y + h);
            c.drawRect(t.rect, t.fill(color));
        }
    }

    public static void demoBadge(Canvas c, Theme t, float right, float baseline) {
        c.drawText("DEMO", right, baseline, t.text(Theme.ACCENT, 10f, Paint.Align.RIGHT, true));
    }

    private static int colorFor(float value, float warnFrom, float alertFrom) {
        if (!Float.isNaN(alertFrom) && value >= alertFrom) {
            return Theme.ALERT;
        }
        if (!Float.isNaN(warnFrom) && value >= warnFrom) {
            return Theme.WARN;
        }
        return Theme.WHITE;
    }

    /* ------------------------------------------------------------------ */
    /* arc gauges                                                          */
    /* ------------------------------------------------------------------ */

    public static void arcGauge(Canvas c, Theme t, float cx, float cy, float r,
                                Channel ch, float min, float max, int decimals,
                                int majorTicks, float redlineFrom, String caption,
                                float valueScale) {
        RectF arc = t.rect;

        // Outer track.
        float trackWidth = r * 0.085f;
        arc.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(arc, ARC_START, ARC_SWEEP, false, t.stroke(Theme.EDGE, trackWidth));

        // Redline segment, if the gauge has one.
        if (!Float.isNaN(redlineFrom) && redlineFrom > min && redlineFrom < max) {
            float from = ARC_START + ARC_SWEEP * (redlineFrom - min) / (max - min);
            float sweep = ARC_START + ARC_SWEEP - from;
            c.drawArc(arc, from, sweep, false, t.stroke(Theme.ALERT, trackWidth));
        }

        // Ticks and their numbers.
        float tickOuter = r - trackWidth * 0.75f;
        float tickInner = tickOuter - r * 0.09f;
        for (int i = 0; i <= majorTicks; i++) {
            float f = (float) i / majorTicks;
            float angle = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            boolean hot = !Float.isNaN(redlineFrom) && (min + (max - min) * f) >= redlineFrom;
            c.drawLine(cx + cos * tickInner, cy + sin * tickInner,
                    cx + cos * tickOuter, cy + sin * tickOuter,
                    t.stroke(hot ? Theme.ALERT : Theme.SILVER, 2f));

            float labelRadius = tickInner - r * 0.11f;
            String tickText = Channel.format((min + (max - min) * f) / valueScale,
                    valueScale >= 1000f ? 0 : (max - min <= 6f ? 1 : 0));
            c.drawText(tickText, cx + cos * labelRadius, cy + sin * labelRadius + 4f,
                    t.text(hot ? Theme.ALERT : Theme.DIM, r * 0.105f, Paint.Align.CENTER, false));
        }

        // Minor ticks between the majors.
        for (int i = 0; i < majorTicks * 2; i++) {
            if (i % 2 == 0) {
                continue;
            }
            float f = (float) i / (majorTicks * 2);
            float angle = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            c.drawLine(cx + cos * (tickOuter - r * 0.045f), cy + sin * (tickOuter - r * 0.045f),
                    cx + cos * tickOuter, cy + sin * tickOuter,
                    t.stroke(Theme.EDGE, 1.5f));
        }

        boolean has = ch.hasValue();
        float f = has ? (ch.getValue() - min) / (max - min) : 0f;
        f = f < 0f ? 0f : (f > 1f ? 1f : f);

        if (has) {
            // Value arc, then a thin needle over it.
            int sweepColor = !Float.isNaN(redlineFrom) && ch.getValue() >= redlineFrom
                    ? Theme.ALERT : Theme.ACCENT;
            c.drawArc(arc, ARC_START, ARC_SWEEP * f, false, t.stroke(sweepColor, trackWidth));

            float angle = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            c.drawLine(cx - cos * r * 0.12f, cy - sin * r * 0.12f,
                    cx + cos * (tickInner - r * 0.02f), cy + sin * (tickInner - r * 0.02f),
                    t.stroke(Theme.WHITE, 2.5f));
            c.drawCircle(cx, cy, r * 0.055f, t.fill(Theme.SILVER));
            c.drawCircle(cx, cy, r * 0.03f, t.fill(Theme.BG));
        }

        // Caption above the hub, value below it.
        c.drawText(caption, cx, cy - r * 0.30f,
                t.text(Theme.SILVER, r * 0.125f, Paint.Align.CENTER, false));

        String value = has ? ch.text(decimals) : NO_DATA;
        c.drawText(value, cx, cy + r * 0.44f,
                t.text(has ? Theme.WHITE : Theme.DIM, r * (has ? 0.30f : 0.15f),
                        Paint.Align.CENTER, true));

        if (has && ch.unit.length() > 0) {
            c.drawText(ch.unit, cx, cy + r * 0.62f,
                    t.text(Theme.DIM, r * 0.11f, Paint.Align.CENTER, false));
        }
        if (ch.isDemo()) {
            c.drawText("DEMO", cx, cy + r * 0.80f,
                    t.text(Theme.ACCENT, r * 0.10f, Paint.Align.CENTER, true));
        }
    }
}
