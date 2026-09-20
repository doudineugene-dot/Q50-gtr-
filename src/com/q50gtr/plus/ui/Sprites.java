package com.q50gtr.plus.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * Статические декоративные растры, вырезанные из утверждённого эталона:
 * задний вид Q50 и стойка пневмоподвески (reference/ui-master ->
 * res/drawable-nodpi, см. tools/uispec/extract_sprites.py).
 *
 * Здесь лежит только неподвижная графика. Всё, что меняется на ходу —
 * стрелки, числа, активная вкладка — по-прежнему рисуется кодом, и целый
 * экран эталона фоном не подкладывается.
 *
 * Ресурсы ищутся по имени, а не через R: тот же код работает и в APK, и в
 * офлайн-рендере, где R не генерируется.
 */
public final class Sprites {

    private static Bitmap dialBezel;
    private static Bitmap carRear;
    private static Bitmap airStrut;
    private static boolean loaded;

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
        String pkg = context.getPackageName();
        dialBezel = decode(res, "dial_bezel", pkg);
        carRear = decode(res, "q50_rear", pkg);
        airStrut = decode(res, "air_strut", pkg);
    }

    private static Bitmap decode(Resources res, String name, String pkg) {
        int id = res.getIdentifier(name, "drawable", pkg);
        return id == 0 ? null : BitmapFactory.decodeResource(res, id);
    }

    public static boolean hasBezel() {
        return dialBezel != null;
    }

    /**
     * Металлическое кольцо прибора. Растр вырезан кольцом (r 112..135 при
     * DIAL_R = 128), поэтому деления, подписи и стрелка под ним не прячутся.
     */
    public static void bezel(Canvas c, Theme t, float cx, float cy, float r) {
        float k = r / 128f;
        float half = dialBezel.getWidth() * 0.5f * k;
        draw(c, t, dialBezel, cx - half, cy - half, half * 2f, half * 2f);
    }

    public static boolean hasCar() {
        return carRear != null;
    }

    public static boolean hasStrut() {
        return airStrut != null;
    }

    public static void car(Canvas c, Theme t, float x, float y, float w, float h) {
        draw(c, t, carRear, x, y, w, h);
    }

    public static void strut(Canvas c, Theme t, float x, float y, float w, float h) {
        draw(c, t, airStrut, x, y, w, h);
    }

    private static void draw(Canvas c, Theme t, Bitmap b,
                             float x, float y, float w, float h) {
        if (b == null) {
            return;
        }
        RectF dst = t.rect;
        dst.set(x, y, x + w, y + h);
        c.drawBitmap(b, dst, null);
    }
}
