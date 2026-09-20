package android.graphics;

import android.content.res.Resources;

import java.io.File;
import javax.imageio.ImageIO;

public final class BitmapFactory {

    public static final class Options {
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inScaled = true;
    }

    private BitmapFactory() { }

    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        return decodeResource(res, id);
    }

    public static Bitmap decodeResource(Resources res, int id) {
        try {
            File f = res.fileFor(id);
            return f == null ? null : new Bitmap(ImageIO.read(f));
        } catch (Exception e) {
            return null;
        }
    }
}
