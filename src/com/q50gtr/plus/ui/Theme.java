package com.q50gtr.plus.ui;

import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Палитра и кисти приборной панели: тёмно-синий фон, металлические ободы
 * приборов, белые шкалы, синий акцент штатного InTouch.
 *
 * Всё выделяется один раз. onDraw идёт десять раз в секунду на железе
 * 2011 года, поэтому в нём не должно быть ни одного new.
 */
public final class Theme {

    /* фон и панели */
    public static final int BG_TOP = 0xFF0A1018;
    public static final int BG_BOTTOM = 0xFF04070C;
    public static final int BAR = 0xFF080D14;
    public static final int TILE_TOP = 0xFF121A26;
    public static final int TILE_BOTTOM = 0xFF0A0F17;
    public static final int TILE_EDGE = 0xFF1E2A3A;

    /* циферблат */
    public static final int DIAL_IN = 0xFF16233A;
    public static final int DIAL_OUT = 0xFF05080E;
    public static final int BEZEL_HI = 0xFFB9C2CE;
    public static final int BEZEL_MID = 0xFF3C444F;
    public static final int BEZEL_LO = 0xFF6E7784;

    /* текст и шкалы */
    public static final int WHITE = 0xFFF2F5FA;
    public static final int TICK = 0xFFDFE4EE;
    public static final int TICK_MINOR = 0xFFB9B4E8;
    public static final int LABEL = 0xFF8A94A4;
    public static final int VALUE_DIM = 0xFF5D6675;

    /* акценты */
    public static final int ACCENT = 0xFF2E7BD6;
    public static final int ACCENT_DEEP = 0xFF16406F;
    public static final int VIOLET = 0xFF9AA2E8;
    public static final int RED = 0xFFD43A32;
    public static final int WARN = 0xFFE8A33A;

    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public final RectF rect = new RectF();
    public final RectF rect2 = new RectF();
    public final Path path = new Path();

    public final Typeface regular;
    public final Typeface bold;

    private Shader bgShader;
    private Shader tileShader;
    private float tileShaderH = -1f;

    /* Шейдеры циферблата зависят только от радиуса: приборы рисуются в
     * локальных координатах, так что одного комплекта хватает на все. */
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

    /** Вертикальный градиент фона панели. */
    public Paint background(float h) {
        if (bgShader == null) {
            bgShader = new LinearGradient(0f, 0f, 0f, h, BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP);
        }
        fill.setShader(bgShader);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }

    /** Градиент плитки. Высота плиток в макете повторяется, поэтому хватает одного. */
    public Paint tile(float h) {
        if (tileShader == null || tileShaderH != h) {
            tileShader = new LinearGradient(0f, 0f, 0f, h, TILE_TOP, TILE_BOTTOM, Shader.TileMode.CLAMP);
            tileShaderH = h;
        }
        fill.setShader(tileShader);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }

    private void ensureDialShaders(float r) {
        if (shaderRadius == r) {
            return;
        }
        // Обод: диагональный «металл» — светлый верх, тёмная середина, блик снизу.
        bezelShader = new LinearGradient(-r, -r, r, r,
                new int[]{BEZEL_HI, BEZEL_MID, BEZEL_LO, BEZEL_MID, BEZEL_HI},
                new float[]{0f, 0.3f, 0.5f, 0.7f, 1f}, Shader.TileMode.CLAMP);
        dialShader = new RadialGradient(0f, -r * 0.15f, r,
                DIAL_IN, DIAL_OUT, Shader.TileMode.CLAMP);
        shaderRadius = r;
    }

    /** Кисть обода. Прибор должен быть уже центрирован в (0,0). */
    public Paint bezel(float r) {
        ensureDialShaders(r);
        fill.setShader(bezelShader);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }

    /** Кисть циферблата. Прибор должен быть уже центрирован в (0,0). */
    public Paint dial(float r) {
        ensureDialShaders(r);
        fill.setShader(dialShader);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }
}
