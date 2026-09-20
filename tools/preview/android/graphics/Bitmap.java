package android.graphics;

import java.awt.image.BufferedImage;

/** Подмена android.graphics.Bitmap поверх Java2D — только для офлайн-рендера. */
public final class Bitmap {

    public enum Config { ARGB_8888, RGB_565 }

    public final BufferedImage image;

    public Bitmap(BufferedImage image) { this.image = image; }

    public int getWidth() { return image.getWidth(); }
    public int getHeight() { return image.getHeight(); }
}
