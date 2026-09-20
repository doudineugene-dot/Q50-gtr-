package com.q50gtr.plus.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Единственный растр в приложении: вид Q50 сзади на вкладке ШАССИ.
 *
 * Всё остальное — приборы, шкалы, деления, текст, карточки, панели —
 * рисуется Canvas в конечном размере, поэтому масштабируется без потери
 * резкости. Автомобиль так нарисовать нельзя: на эталоне это фотография.
 *
 * Растр хранится в исходном разрешении эталона (198x138) и увеличивается
 * не более чем примерно в полтора раза, с билинейной фильтрацией. Если
 * ресурс не прочитался, рисуется векторный запасной вариант.
 */
public final class Sprites {

    private static Bitmap carRear;
    private static boolean loaded;

    private static final Paint PAINT =
            new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private static final RectF DST = new RectF();

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
        int id = res.getIdentifier("q50_rear", "drawable", context.getPackageName());
        if (id != 0) {
            carRear = BitmapFactory.decodeResource(res, id, opts);
        }
    }

    public static boolean hasCar() {
        return carRear != null;
    }

    public static void car(Canvas c, float x, float y, float w, float h) {
        if (carRear == null) {
            return;
        }
        DST.set(x, y, x + w, y + h);
        c.drawBitmap(carRear, null, DST, PAINT);
    }
}
