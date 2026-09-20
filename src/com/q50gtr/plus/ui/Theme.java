package com.q50gtr.plus.ui;

import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Палитра снята пипеткой с master reference, см. docs/UI-MASTER-SPEC.md.
 * Всё выделяется один раз: onDraw идёт 10 раз в секунду на железе 2011 года.
 */
public final class Theme {

    public static final int BG = 0xFF020408;
    public static final int NAV = 0xFF000005;
    public static final int HAIRLINE = 0xFF2A2740;

    public static final int TILE = 0xFF070C12;
    public static final int TILE_EDGE = 0xFF1B2430;

    public static final int DIAL_IN = 0xFF101C33;
    public static final int DIAL_OUT = 0xFF000006;
    public static final int RING_DARK = 0xFF05070C;

    public static final int BEZEL_HI = 0xFF9BA5B3;
    public static final int BEZEL_MID = 0xFF232932;
    public static final int BEZEL_LO = 0xFF5B6470;

    public static final int WHITE = 0xFFF2F5FA;
    public static final int TICK = 0xFFF3EDF6;
    public static final int TICK_MINOR = 0xFF8E8AA8;
    public static final int LABEL = 0xFF8A94A4;
    public static final int VALUE_DIM = 0xFF4A5462;

    public static final int ACCENT = 0xFF8E86FF;
    public static final int TAB_ACTIVE = 0xFF303750;
    public static final int RED = 0xFF9E1219;
    public static final int WARN = 0xFFE8A33A;

    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public final RectF rect = new RectF();
    public final RectF rect2 = new RectF();
    public final Path path = new Path();

    public final Typeface regular;
    public final Typeface bold;

    private Shader bezelShader;
    private Shader dialShader;
    private float shaderRadius = -1f;

    public Theme() {
        Typeface cond = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        regular = cond != null ? cond : Typeface.SANS_SERIF;
        Typeface condBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        bold = condBold != null ? condBold : Typeface.DEFAULT_BOLD;

        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.BUTT);
        text.setStyle(Paint.Style.FILL);
        text.setTypeface(regular);
    }

    public Paint fill(int color) {
        fill.setShader(null);
        fill.setColor(color);
        return fill;
    }

    public Paint stroke(int color, float width) {
        stroke.setShader(null);
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        return stroke;
    }

    public Paint text(int color, float size, Paint.Align align, boolean boldFace) {
        text.setShader(null);
        text.setColor(color);
        text.setTextSize(size);
        text.setTextAlign(align);
        text.setTypeface(boldFace ? bold : regular);
        return text;
    }

    private void ensureDialShaders(float r) {
        if (shaderRadius == r) {
            return;
        }
        bezelShader = new LinearGradient(-r, -r, r * 0.6f, r,
                new int[]{BEZEL_HI, BEZEL_LO, BEZEL_MID, BEZEL_LO, BEZEL_HI},
                new float[]{0f, 0.22f, 0.52f, 0.78f, 1f}, Shader.TileMode.CLAMP);
        // Циферблат почти чёрный, с лёгким синим подсветом у центра.
        dialShader = new RadialGradient(0f, -r * 0.10f, r * 0.95f,
                DIAL_IN, DIAL_OUT, Shader.TileMode.CLAMP);
        shaderRadius = r;
    }

    public Paint bezel(float r) {
        ensureDialShaders(r);
        fill.setShader(bezelShader);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }

    public Paint dial(float r) {
        ensureDialShaders(r);
        fill.setShader(dialShader);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }
}
