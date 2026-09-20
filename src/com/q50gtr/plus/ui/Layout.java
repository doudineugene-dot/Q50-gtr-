package com.q50gtr.plus.ui;

/**
 * Раскладка под фактический размер окна, а не под выдуманный.
 *
 * Пропорции эталона (548x480 полезной области) и пропорции экрана ГУ не
 * совпадают. Растянуть всё по X нельзя — приборы станут овальными. Поэтому:
 *
 *   вертикаль и размеры  — единый масштаб s = высота / 480;
 *   горизонталь          — раздаётся по всей фактической ширине.
 *
 * Круги остаются кругами, боковых полей нет. Числа взяты из
 * docs/UI-MASTER-SPEC.md, то есть измерены по эталону.
 */
public final class Layout {

    /** Высота эталона. Ширина намеренно не константа: её даёт окно. */
    public static final float DESIGN_H = 480f;

    /* --- вертикаль в единицах эталона --- */
    private static final float TOP_H = 44f;
    private static final float NAV_H = 48f;
    private static final float DIAL_CY = 181f;
    private static final float DIAL_R = 128f;
    private static final float TILE_Y = 304f;
    private static final float TILE_Y_CHASSIS = 331f;
    private static final float TILE_H = 92f;

    /* --- горизонтальные отступы в единицах эталона --- */
    private static final float SIDE = 9f;
    private static final float TILE_GAP = 6f;
    private static final float ARROW_SIDE = 30.5f;

    /** Фактический размер окна. */
    public final float w;
    public final float h;
    /** Единый масштаб: всё, что имеет размер, умножается на него. */
    public final float s;

    public final float topH;
    public final float navH;
    public final float dialCy;
    public final float dialR;
    public final float tileY;
    public final float tileYChassis;
    public final float tileH;
    public final float tileW;
    public final float side;
    public final float tileGap;

    public Layout(float width, float height) {
        w = width <= 0f ? 840f : width;
        h = height <= 0f ? DESIGN_H : height;
        s = h / DESIGN_H;

        topH = TOP_H * s;
        navH = NAV_H * s;
        dialCy = DIAL_CY * s;
        tileY = TILE_Y * s;
        tileYChassis = TILE_Y_CHASSIS * s;
        tileH = TILE_H * s;
        side = SIDE * s;
        tileGap = TILE_GAP * s;
        tileW = (w - 2f * side - 3f * tileGap) / 4f;

        // Радиус ограничен по вертикали: сверху статус-полоса, снизу карточки.
        // По горизонтали места теперь заведомо хватает, так что бьётся именно
        // высота — ровно как на эталоне.
        float byHeight = (tileY - topH) * 0.5f + 5f * s;
        float wanted = DIAL_R * s;
        dialR = wanted < byHeight ? wanted : byHeight;
    }

    /** Центры приборов: четверть и три четверти фактической ширины. */
    public float dialCx(int index) {
        return index == 0 ? w * 0.25f : w * 0.75f;
    }

    public float tileX(int index) {
        return side + index * (tileW + tileGap);
    }

    public float contentY() {
        return topH;
    }

    public float contentH() {
        return h - topH - navH;
    }

    public float navTop() {
        return h - navH;
    }

    /* --- верхняя полоса: края по краям окна, середина по долям ширины --- */
    public float backCx() {
        return ARROW_SIDE * s;
    }

    public float clockCx() {
        return w * 0.43f;
    }

    public float tempCx() {
        return w * 0.63f;
    }

    public float demoCx() {
        return w * 0.775f;
    }

    public float signalCx() {
        return w - 60f * s;
    }

    public float bluetoothCx() {
        return w - 34f * s;
    }

    /* --- навигация --- */
    public float navArrowLeftCx() {
        return ARROW_SIDE * s;
    }

    public float navArrowRightCx() {
        return w - ARROW_SIDE * s;
    }

    public float tabSide() {
        return 61f * s;
    }

    public float tabPitch() {
        return (w - 2f * tabSide()) / 3f;
    }

    public float tabCx(int index) {
        return tabSide() + tabPitch() * (index + 0.5f);
    }

    /* --- ШАССИ --- */
    public float chassisSplitX() {
        return w * 0.63f;
    }

    public float chassisColX() {
        return chassisSplitX();
    }

    public float chassisColW() {
        return w - side - chassisColX();
    }

    public float chassisCardY(int index) {
        return (58f + index * 68.25f) * s;
    }

    public float chassisCardH() {
        return 64f * s;
    }

    /** Кегль текста в единицах эталона, приведённый к экрану. */
    public float text(float designSize) {
        return designSize * s;
    }
}
