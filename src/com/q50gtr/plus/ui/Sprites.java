package com.q50gtr.plus.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Неподвижная декоративная графика, вырезанная из утверждённого эталона
 * (tools/uispec/make_sprites.py).
 *
 * Сюда попадает только то, что примитивами убедительно не нарисовать:
 * полированное кольцо прибора, автомобиль и стойка пневмоподвески. Шкалы,
 * деления, цифры, стрелки, карточки и панели рисует Canvas.
 *
 * Растры всегда кладутся в своём соотношении сторон и при штатной геометрии
 * окна (800x480) выходят почти один к одному со своим исходным размером —
 * никакого заметного пересэмплинга.
 */
public final class Sprites {

    private static Bitmap dialBezel;
    private static Bitmap carRear;
    private static Bitmap airStrut;
    private static boolean loaded;

    private static final Paint PAINT =
            new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private static final RectF DST = new RectF();

    /** Доля радиуса прибора, которую занимает половина растра кольца. */
    private static final float BEZEL_SPAN = 133f / 128f;

    private Sprites() {
    }

    public static void load(Context context) {
        if (loaded || context == null) {
            return;
        }
        loaded = true;
        Resources res = context.getResources();
        if (res == null) {
            return;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        opts.inDither = true;
        dialBezel = decode(res, context, "dial_bezel", opts);
        carRear = decode(res, context, "q50_rear", opts);
        airStrut = decode(res, context, "air_strut", opts);
    }

    private static Bitmap decode(Resources res, Context ctx, String name,
                                 BitmapFactory.Options opts) {
        int id = res.getIdentifier(name, "drawable", ctx.getPackageName());
        return id == 0 ? null : BitmapFactory.decodeResource(res, id, opts);
    }

    public static boolean hasBezel() {
        return dialBezel != null;
    }

    public static boolean hasCar() {
        return carRear != null;
    }

    public static boolean hasStrut() {
        return airStrut != null;
    }

    /** Кольцо прибора вокруг центра (cx, cy) с радиусом циферблата r. */
    public static void bezel(Canvas c, float cx, float cy, float r) {
        if (dialBezel == null) {
            return;
        }
        float half = r * BEZEL_SPAN;
        draw(c, dialBezel, cx - half, cy - half, half * 2f, half * 2f);
    }

    /** Высота автомобиля при заданной ширине, в своём соотношении сторон. */
    public static float carHeightFor(float width) {
        if (carRear == null) {
            return width * 138f / 198f;
        }
        return width * carRear.getHeight() / (float) carRear.getWidth();
    }

    public static void car(Canvas c, float x, float y, float w) {
        draw(c, carRear, x, y, w, carHeightFor(w));
    }

    /** Ширина стойки при заданной высоте, в своём соотношении сторон. */
    public static float strutWidthFor(float height) {
        if (airStrut == null) {
            return height * 27f / 119f;
        }
        return height * airStrut.getWidth() / (float) airStrut.getHeight();
    }

    public static void strut(Canvas c, float cx, float y, float h) {
        float w = strutWidthFor(h);
        draw(c, airStrut, cx - w * 0.5f, y, w, h);
    }

    private static void draw(Canvas c, Bitmap b, float x, float y, float w, float h) {
        if (b == null) {
            return;
        }
        DST.set(x, y, x + w, y + h);
        c.drawBitmap(b, null, DST, PAINT);
    }
}
