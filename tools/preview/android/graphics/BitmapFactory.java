package android.graphics;

import android.content.res.Resources;

import java.io.File;
import javax.imageio.ImageIO;

public final class BitmapFactory {
    private BitmapFactory() { }

    public static Bitmap decodeResource(Resources res, int id) {
        try {
            File f = res.fileFor(id);
            return f == null ? null : new Bitmap(ImageIO.read(f));
        } catch (Exception e) {
            return null;
        }
    }
}
