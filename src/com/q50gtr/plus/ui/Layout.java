package com.q50gtr.plus.ui;

/**
 * Координаты динамического слоя, снятые с MASTER.
 *
 * Фон экрана — растр 840x480, полученный из утверждённого эталона
 * (tools/uispec/make_backgrounds.py). Поэтому система координат здесь —
 * пиксели самого эталона, а не абстрактная сетка: всё, что рисует Canvas,
 * должно лечь ровно поверх фона.
 *
 * Кадрирование исходного коллажа уехало по экранам, поэтому левая кромка
 * рамки у каждого своя — см. {@link #frameX} и docs/UI-MASTER-SPEC.md, п.1.
 */
public final class Layout {

    public static final float SCREEN_W = 840f;
    public static final float SCREEN_H = 480f;
    /** Полезная ширина эталона внутри кадра: остальное — чёрные поля. */
    public static final float CONTENT_W = 536f;

    /** Левая кромка рамки экрана внутри кадра, по вкладкам. */
    private static final float[] FRAME_X = {152f, 163f, 174f};

    /* --- приборы (спека, п.5) --- */
    public static final float DIAL_CY = 181f;
    public static final float DIAL_R = 128f;
    private static final float[] DIAL_CX = {137f, 409f};

    /* --- карточки (спека, п.6) --- */
    public static final float TILE_X0 = 9f;
    public static final float TILE_PITCH = 134f;
    /**
     * Значение в карточке начинается на этом отступе от её левого края.
     * На ШАССИ эталон ставит его на 11 px правее, чем на двух других вкладках.
     */
    private static final float[] TILE_VALUE_DX = {48f, 48f, 59f};
    public static final float TILE_VALUE_BASE = 372f;
    public static final float TILE_VALUE_BASE_CHASSIS = 399f;

    /* --- верхняя полоса (спека, п.4) --- */
    public static final float CLOCK_CX = 230.5f;
    public static final float CLOCK_BASE = 36f;
    public static final float TEMP_CX = 346.5f;
    public static final float DEMO_CX = 425f;

    /* --- ШАССИ (спека, п.8) --- */
    private static final float[] STRUT_CX = {44f, 296f};
    public static final float PRESS_BASE_FRONT = 120f;
    public static final float PRESS_BASE_REAR = 283f;
    public static final float COL_VALUE_X = 417f;
    private static final float[] COL_VALUE_BASE = {111f, 181f, 249f, 316f};

    /* --- навигация: только для попадания пальцем, рисует её фон --- */
    public static final float NAV_TOP = 432f;
    private static final float TAB_SIDE = 61f;
    private static final float TAB_PITCH = (548f - 2f * TAB_SIDE) / 3f;

    /** Подписи приборов на ТОПЛИВЕ эталон рисует на 6 px ниже. */
    private static final float[] CAPTION_DY = {0f, 6f, 0f};

    private final int index;
    private final float fx;

    public Layout(int page) {
        index = page < 0 ? 0 : (page > 2 ? 2 : page);
        fx = FRAME_X[index];
    }

    public float captionDy() {
        return CAPTION_DY[index];
    }

    /** Левая кромка рамки этой вкладки в кадре 840x480. */
    public float frameX() {
        return fx;
    }

    public float x(float frameRelative) {
        return fx + frameRelative;
    }

    public float dialCx(int index) {
        return fx + DIAL_CX[index];
    }

    public float tileValueX(int i) {
        return fx + TILE_X0 + i * TILE_PITCH + TILE_VALUE_DX[index];
    }

    public float strutCx(int index) {
        return fx + STRUT_CX[index];
    }

    public float colValueBase(int index) {
        return COL_VALUE_BASE[index];
    }

    public float tabCx(int index) {
        return fx + TAB_SIDE + TAB_PITCH * (index + 0.5f);
    }

    public float tabHalfWidth() {
        return TAB_PITCH * 0.5f;
    }
}
