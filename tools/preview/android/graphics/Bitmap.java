package android.graphics;

import java.awt.image.BufferedImage;

/** Подмена android.graphics.Bitmap поверх Java2D — только для офлайн-рендера. */
public final class Bitmap {

    public enum Config { ARGB_8888, RGB_565 }

    public final BufferedImage image;

    public Bitmap(BufferedImage image) { this.image = image; }

    public int getWidth() { return image.getWidth(); }
    public int getHeight() { return image.getHeight(); }

    /* Приборы никогда не декодируют и не освобождают растры сами — эти два
     * метода нужны только AlpFrameAssembler (трансляция экрана ALP3), а не
     * трём утверждённым страницам, которые проверяет офлайн-рендер. Здесь —
     * минимальная заглушка, чтобы дерево исходников компилировалось целиком. */
    private boolean recycled;

    public boolean isRecycled() { return recycled; }
    public void recycle() { recycled = true; }
}
