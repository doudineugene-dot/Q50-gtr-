package android.graphics;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.geom.Point2D;

public class LinearGradient extends Shader {
    private final float x0, y0, x1, y1;
    private final int[] colors;
    private final float[] stops;

    public LinearGradient(float x0, float y0, float x1, float y1, int c0, int c1, TileMode m) {
        this(x0, y0, x1, y1, new int[]{c0, c1}, new float[]{0f, 1f}, m);
    }

    public LinearGradient(float x0, float y0, float x1, float y1,
                          int[] colors, float[] stops, TileMode m) {
        this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
        this.colors = colors; this.stops = stops;
    }

    @Override
    public java.awt.Paint awt() {
        Color[] cs = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            cs[i] = new Color(colors[i], true);
        }
        float[] fr = new float[stops.length];
        for (int i = 0; i < stops.length; i++) {
            fr[i] = stops[i] <= 0f ? 0.0001f * i : stops[i];
            if (i > 0 && fr[i] <= fr[i - 1]) {
                fr[i] = fr[i - 1] + 0.0001f;
            }
        }
        if (Math.abs(x1 - x0) < 0.01f && Math.abs(y1 - y0) < 0.01f) {
            return cs[0];
        }
        return new LinearGradientPaint(new Point2D.Float(x0, y0), new Point2D.Float(x1, y1),
                fr, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }
}
