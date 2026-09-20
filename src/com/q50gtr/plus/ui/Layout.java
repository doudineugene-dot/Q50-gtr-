package com.q50gtr.plus.ui;

/**
 * Вся геометрия экрана в одном месте.
 *
 * Числа взяты из docs/UI-MASTER-SPEC.md, то есть измерены детектором по
 * reference/ui-master/MASTER_*_840x480.png, а не подобраны на глаз.
 *
 * Рамка эталона — 548x480 (1.142:1), головное устройство Q50 — 840x480
 * (1.75:1). Поэтому раскладка параметрическая: вертикали, радиусы и высоты
 * фиксированы в пикселях эталона, а горизонталь раздаётся по фактической
 * ширине. При width == MASTER_W геометрия совпадает с эталоном попиксельно —
 * на этом и строится сравнение в tools/uispec/compare.py.
 */
public final class Layout {

    /** Ширина рамки экрана в MASTER. */
    public static final float MASTER_W = 548f;
    /** Ширина реального головного устройства. */
    public static final float DEVICE_W = 840f;
    public static final float SCREEN_H = 480f;

    /* --- вертикаль: одинакова и в эталоне, и на устройстве --- */
    public static final float TOP_H = 44f;
    public static final float NAV_H = 48f;
    public static final float CONTENT_Y = TOP_H;
    public static final float CONTENT_H = SCREEN_H - TOP_H - NAV_H;   // 388

    /** Приборы: центр и внешний радиус безеля (спека, п.5). */
    public static final float DIAL_CY = 181f - TOP_H;
    public static final float DIAL_R = 128f;

    /** Карточки (спека, п.6). */
    public static final float TILE_INSET = 9f;
    public static final float TILE_GAP = 6f;
    public static final float TILE_H = 92f;
    public static final float TILE_Y = 304f - TOP_H;
    /** Нижний ряд ШАССИ стоит ниже: спека, п.8. */
    public static final float TILE_Y_CHASSIS = 331f - TOP_H;

    /** Навигация (спека, п.7): боковое поле под стрелки. */
    public static final float TAB_SIDE = 61f;
    public static final float TAB_BAND_Y = 440f;
    public static final float TAB_BAND_H = 32f;

    public final float w;
    public final float tileW;
    public final float tabPitch;
    public final float tabW;

    public Layout(float width) {
        this.w = width;
        this.tileW = (width - 2f * TILE_INSET - 3f * TILE_GAP) / 4f;
        this.tabPitch = (width - 2f * TAB_SIDE) / 3f;
        this.tabW = tabPitch * (146f / 142f);
    }

    public static Layout master() {
        return new Layout(MASTER_W);
    }

    public static Layout device() {
        return new Layout(DEVICE_W);
    }

    /* ------------------------------------------------------------------ */

    /** Центры приборов: четверть и три четверти ширины (спека, п.5). */
    public float dialCx(int index) {
        return index == 0 ? w * 0.25f : w * 0.75f;
    }

    public float tileX(int index) {
        return TILE_INSET + index * (tileW + TILE_GAP);
    }

    public float tabCx(int index) {
        return TAB_SIDE + tabPitch * (index + 0.5f);
    }

    /* --- статус-бар (спека, п.4) --- */
    public float backCx() {
        return 30.5f;
    }

    public float clockCx() {
        return w * (230.5f / MASTER_W);
    }

    public float tempCx() {
        return w * (346.5f / MASTER_W);
    }

    public float signalCx() {
        return w - 60.5f;
    }

    public float bluetoothCx() {
        return w - 36.5f;
    }

    /* --- навигация --- */
    public float navArrowLeftCx() {
        return 30.5f;
    }

    public float navArrowRightCx() {
        return w - 33.5f;
    }

    /* --- ШАССИ (спека, п.8), координаты внутри контента --- */
    public float chassisSplitX() {
        return w * 0.63f;
    }

    public float chassisColX() {
        return chassisSplitX();
    }

    public float chassisColW() {
        return w - TILE_INSET - chassisColX();
    }

    public float chassisCardY(int index) {
        return 58f - TOP_H + index * 68.25f;
    }

    public float chassisCardH() {
        return 64f;
    }
}
