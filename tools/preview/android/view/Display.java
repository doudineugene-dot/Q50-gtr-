package android.view;

import android.util.DisplayMetrics;

public class Display {
    public int getWidth() { return 840; }
    public int getHeight() { return 480; }
    public int getOrientation() { return 0; }
    public void getMetrics(DisplayMetrics m) {
        m.widthPixels = 840; m.heightPixels = 480;
        m.density = 1f; m.densityDpi = 160; m.scaledDensity = 1f;
        m.xdpi = 160f; m.ydpi = 160f;
    }
}
