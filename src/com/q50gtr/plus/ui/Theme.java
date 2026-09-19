package com.q50gtr.plus.ui;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Colours, type and the handful of reusable Paint objects the dashboard draws
 * with. Everything is allocated once: onDraw runs ten times a second on a head
 * unit that is a decade old, so it allocates nothing.
 *
 * The palette is the instrument cluster of the car, not Material: near-black
 * background, white and silver scales, one thin violet-blue accent.
 */
public final class Theme {

    public static final int BG = 0xFF04060A;
    public static final int PANEL = 0xFF0A0E14;
    public static final int PANEL_TOP = 0xFF10151E;
    public static final int EDGE = 0xFF1B2230;
    public static final int EDGE_SOFT = 0xFF121823;

    public static final int WHITE = 0xFFF2F5F8;
    public static final int SILVER = 0xFFB7BEC8;
    public static final int DIM = 0xFF5A6473;
    public static final int ACCENT = 0xFF7A6CFF;
    public static final int ACCENT_DEEP = 0xFF2E2680;
    public static final int WARN = 0xFFFFB020;
    public static final int ALERT = 0xFFFF4438;

    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public final RectF rect = new RectF();
    public final RectF rect2 = new RectF();
    public final Path path = new Path();

    public final Typeface regular;
    public final Typeface bold;

    public Theme() {
        Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        regular = condensed != null ? condensed : Typeface.SANS_SERIF;
        Typeface condensedBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        bold = condensedBold != null ? condensedBold : Typeface.DEFAULT_BOLD;

        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.BUTT);
        text.setStyle(Paint.Style.FILL);
        text.setTypeface(regular);
    }

    public Paint fill(int color) {
        fill.setColor(color);
        return fill;
    }

    public Paint stroke(int color, float width) {
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        return stroke;
    }

    public Paint text(int color, float size, Paint.Align align, boolean boldFace) {
        text.setColor(color);
        text.setTextSize(size);
        text.setTextAlign(align);
        text.setTypeface(boldFace ? bold : regular);
        return text;
    }
}
