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

    /* Карточка на эталоне не залита ровно: сверху 19,24,31, в середине
     * 4,7,11, снизу 16,20,27, кромка доходит до 77,82,90. */
    public static final int TILE = 0xFF04070B;
    public static final int TILE_TOP = 0xFF131820;
    public static final int TILE_BOTTOM = 0xFF10141B;
    public static final int TILE_EDGE = 0xFF454B55;

    /* Циферблат на эталоне почти чёрный: пипетка по радиусу даёт 2..10 по
     * всем каналам, никакого синего подсвета в полсилы там нет. */
    public static final int DIAL_IN = 0xFF05080F;
    public static final int DIAL_OUT = 0xFF000103;
    public static final int RING_DARK = 0xFF000002;

    /** Разделитель под статус-полосой: сине-лавандовый, не серый. */
    public static final int RULE = 0xFF8A8EB7;
    /** Фон навигации: сверху светлее, снизу темнее. */
    public static final int NAV_TOP = 0xFF0E141C;
    public static final int NAV_BOTTOM = 0xFF05080C;
    /** Активная вкладка: синий градиент во всю высоту полосы. */
    public static final int TAB_TOP = 0xFF2A3660;
    public static final int TAB_BOTTOM = 0xFF222B55;
    public static final int TAB_EDGE = 0xFF7988B5;

    public static final int BEZEL_HI = 0xFF9BA5B3;
    public static final int BEZEL_MID = 0xFF232932;
    public static final int BEZEL_LO = 0xFF5B6470;

    public static final int WHITE = 0xFFF2F5FA;
    public static final int TICK = 0xFFF3EDF6;
    public static final int TICK_MINOR = 0xFF8E8AA8;
    public static final int LABEL = 0xFF8A94A4;
    public static final int VALUE_DIM = 0xFF4A5462;

    /*
     * Стрелка и ступица по замерам эталона. Ступица там тёмная: радиальный
     * профиль даёт 25..33 по яркости внутри и слабый ободок 25..37 на r≈18.
     */
    public static final int NEEDLE = 0xFFFFFFFF;
    public static final int HUB_RIM = 0xFF2A3037;
    public static final int HUB_FILL = 0xFF0E1317;
    public static final int HUB_CORE = 0xFF080B10;

    public static final int ACCENT = 0xFF8E86FF;
    public static final int TAB_ACTIVE = 0xFF303750;
    public static final int RED = 0xFF9E1219;
    /** Красная зона шкалы: на эталоне она светлее и менее густая, чем алерт. */
    public static final int RED_ZONE = 0xFFBA3E31;
    public static final int WARN = 0xFFE8A33A;

    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    public final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    /*
     * SUBPIXEL_TEXT даёт более точное позиционирование глифов, LINEAR_TEXT
     * убирает ступеньку при дробных кеглях — а они здесь дробные, потому что
     * все размеры домножаются на масштаб окна.
     */
    public final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);

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

    /**
     * Штатный шрифт кластера Q50 — узкий гротеск. Настоящего шрифта Infiniti
     * у нас нет, и на Android 2.3 нет даже семейства sans-serif-condensed:
     * Typeface.create() молча возвращает обычный Droid Sans. Поэтому узость
     * набирается через setTextScaleX — коэффициент подобран по эталону:
     * «11:06» занимает там 47 px при высоте цифр 17 px.
     */
    public static final float CONDENSE = 0.79f;

    public Paint text(int color, float size, Paint.Align align, boolean boldFace) {
        text.setShader(null);
        text.setStyle(Paint.Style.FILL);
        text.setColor(color);
        text.setTextSize(size);
        text.setTextScaleX(CONDENSE);
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

    /*
     * Вертикальные градиенты кэшируются: onDraw идёт 10 раз в секунду, и
     * создавать по шесть шейдеров на кадр на железе 2011 года не стоит.
     * Ключ — четыре параметра градиента; их у панели всего несколько.
     */
    private static final int GRAD_CACHE = 8;
    private final long[] gradKey = new long[GRAD_CACHE];
    private final Shader[] gradShader = new Shader[GRAD_CACHE];
    private int gradNext;

    private Paint cached(long key, int top, int mid, int bottom,
                         float y0, float y1, boolean three) {
        for (int i = 0; i < GRAD_CACHE; i++) {
            if (gradShader[i] != null && gradKey[i] == key) {
                fill.setShader(gradShader[i]);
                fill.setColor(0xFFFFFFFF);
                return fill;
            }
        }
        Shader sh = three
                ? new LinearGradient(0f, y0, 0f, y1,
                        new int[]{top, mid, bottom}, new float[]{0f, 0.45f, 1f},
                        Shader.TileMode.CLAMP)
                : new LinearGradient(0f, y0, 0f, y1, top, bottom,
                        Shader.TileMode.CLAMP);
        gradKey[gradNext] = key;
        gradShader[gradNext] = sh;
        gradNext = (gradNext + 1) % GRAD_CACHE;
        fill.setShader(sh);
        fill.setColor(0xFFFFFFFF);
        return fill;
    }

    /**
     * Тёмная обводка под текстом. Нужна там, где под подписью может оказаться
     * стрелка: белое по белому нечитаемо, а сдвигать подпись нельзя — её
     * положение снято с эталона.
     */
    public Paint textOutline(float size, float width, Paint.Align align, boolean boldFace) {
        text.setShader(null);
        text.setColor(0xC8000000);
        text.setStyle(Paint.Style.STROKE);
        text.setStrokeWidth(width);
        text.setTextSize(size);
        text.setTextScaleX(CONDENSE);
        text.setTextAlign(align);
        text.setTypeface(boldFace ? bold : regular);
        return text;
    }

    /** Трёхточечный вертикальный градиент: заливка карточки. */
    public Paint vertical3(int top, int mid, int bottom, float y0, float y1) {
        long key = ((long) top << 32) ^ ((long) bottom << 8)
                ^ ((long) (y0 * 4f) << 20) ^ (long) (y1 * 4f);
        return cached(key, top, mid, bottom, y0, y1, true);
    }

    /** Вертикальный градиент: полоса навигации, подсветка вкладки. */
    public Paint vertical(int top, int bottom, float y0, float y1) {
        long key = 1L ^ ((long) top << 32) ^ ((long) bottom << 8)
                ^ ((long) (y0 * 4f) << 20) ^ (long) (y1 * 4f);
        return cached(key, top, top, bottom, y0, y1, false);
    }

    /**
     * Яркость безеля по углу, снятая пипеткой с эталона через 15°: кольцо
     * почти везде светлое, но внизу (75..105°) уходит в тень.
     */
    private static final int[] BEZEL_BY_ANGLE = {
            235, 250, 255, 226, 102, 14, 3, 11, 69, 204, 243, 138,
            109, 173, 229, 158, 197, 181, 157, 166, 227, 225, 232, 250};

    /** Интерполированная яркость безеля для угла в градусах. */
    public static int bezelAt(float deg, float scale) {
        float f = ((deg % 360f) + 360f) % 360f / 15f;
        int i = (int) f;
        float k = f - i;
        int a = BEZEL_BY_ANGLE[i % 24];
        int b = BEZEL_BY_ANGLE[(i + 1) % 24];
        int v = (int) ((a + (b - a) * k) * scale);
        // Кольцо слегка тёплое: красный канал чуть выше синего.
        int rr = Math.min(255, v);
        int gg = Math.min(255, (int) (v * 0.975f));
        int bb = Math.min(255, (int) (v * 0.99f));
        return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
    }
}
