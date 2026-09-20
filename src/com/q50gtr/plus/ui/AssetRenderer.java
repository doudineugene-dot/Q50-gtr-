package com.q50gtr.plus.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Статические фоны экранов, подготовленные из утверждённого MASTER.
 *
 * В каждом фоне стёрты только меняющиеся элементы: стрелки, ступицы,
 * цифровые значения приборов и карточек, часы, наружная температура,
 * буква селектора и давления в стойках. Металлические кольца, циферблаты,
 * деления, цифры и единицы шкал, красные зоны, рамки и подписи карточек,
 * иконки, верхняя и нижняя панели, автомобиль и стойки — это фон.
 *
 * Ресурсы ищутся по имени, а не через R: тот же код работает и в APK, и в
 * офлайн-рендере, где R не генерируется.
 */
public final class AssetRenderer {

    private static final String[] NAMES = {
            "engine_bg_clean", "fuel_bg_clean", "chassis_bg_clean"};

    private final Bitmap[] backgrounds = new Bitmap[NAMES.length];
    private final RectF dst = new RectF();

    /**
     * Без этой кисти фон рисовался ближайшим соседом и без сглаживания
     * градиентов: на экране ГУ это давало рваные кромки и полосы на тёмном.
     */
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    public AssetRenderer(Context context) {
        if (context == null) {
            return;
        }
        Resources res = context.getResources();
        if (res == null) {
            return;
        }
        // 565 вместо 8888: у фонов нет альфы, а на Gingerbread это вдвое
        // меньше памяти под растр — 0.8 МБ на экран вместо 1.6 МБ.
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        // Сжатие до 565 без дизеринга кладёт полосы ровно на тёмные градиенты,
        // из которых состоит почти весь фон.
        opts.inDither = true;
        opts.inScaled = false;

        String pkg = context.getPackageName();
        for (int i = 0; i < NAMES.length; i++) {
            int id = res.getIdentifier(NAMES[i], "drawable", pkg);
            if (id != 0) {
                backgrounds[i] = BitmapFactory.decodeResource(res, id, opts);
            }
        }
    }

    public boolean has(int page) {
        return page >= 0 && page < backgrounds.length && backgrounds[page] != null;
    }

    /** Кладёт фон вкладки в кадр 840x480 со смещением offsetX (для свайпа). */
    public void draw(Canvas c, int page, float offsetX) {
        if (!has(page)) {
            return;
        }
        dst.set(offsetX, 0f, offsetX + Layout.SCREEN_W, Layout.SCREEN_H);
        c.drawBitmap(backgrounds[page], null, dst, paint);
    }
}
