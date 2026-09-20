package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.Channel;

/**
 * Координаты из docs/UI-MASTER-SPEC.md. Держим их в одном месте, чтобы все
 * три экрана стояли на одной сетке, как на master reference.
 *
 * Система координат контента: начало под верхней полосой, высота 382.
 */
public final class Layout {

    public static final float SCREEN_W = 840f;
    public static final float TOP_H = 43f;
    public static final float NAV_H = 55f;
    public static final float CONTENT_H = 480f - TOP_H - NAV_H;

    /** Приборы: centerY 171 на экране -> 128 в контенте. */
    public static final float DIAL_CY = 182f - TOP_H;
    public static final float DIAL_R = 139f;
    public static final float DIAL_LEFT_CX = 278f;
    public static final float DIAL_RIGHT_CX = 562f;

    /** Карточки: y 299 на экране -> 256 в контенте. */
    public static final float TILE_Y = 322f - TOP_H;
    public static final float TILE_H = 88f;
    public static final float TILE_MARGIN = 14f;
    public static final float TILE_GAP = 11f;
    public static final float TILE_W =
            (SCREEN_W - TILE_MARGIN * 2f - TILE_GAP * 3f) / 4f;

    private Layout() {
    }

    public static float tileX(int index) {
        return TILE_MARGIN + index * (TILE_W + TILE_GAP);
    }

    public static void tile(Canvas c, Theme t, int index, int icon, String label,
                            Channel ch, int decimals, String unit,
                            float warnFrom, float alertFrom) {
        Gauges.tile(c, t, tileX(index), TILE_Y, TILE_W, TILE_H,
                icon, label, ch, decimals, unit, warnFrom, alertFrom);
    }
}
