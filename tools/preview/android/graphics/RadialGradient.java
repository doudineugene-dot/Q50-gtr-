package android.graphics;

import java.awt.Color;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

public class RadialGradient extends Shader {
    private final float cx, cy, r;
    private final int[] colors;
    private final float[] stops;

    public RadialGradient(float cx, float cy, float r, int c0, int c1, TileMode m) {
        this(cx, cy, r, new int[]{c0, c1}, new float[]{0f, 1f}, m);
    }

    public RadialGradient(float cx, float cy, float r, int[] colors, float[] stops, TileMode m) {
        this.cx = cx; this.cy = cy; this.r = r;
        this.colors = colors;
        if (stops != null) {
            this.stops = stops;
        } else {
            this.stops = new float[colors.length];
            for (int i = 0; i < colors.length; i++) {
                this.stops[i] = colors.length == 1 ? 0f : (float) i / (colors.length - 1);
            }
        }
    }

    @Override
    public java.awt.Paint awt() {
        Color[] cs = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            cs[i] = new Color(colors[i], true);
        }
        if (cs.length == 1) {
            return cs[0];
        }
        float[] fr = new float[stops.length];
        for (int i = 0; i < stops.length; i++) {
            fr[i] = stops[i] <= 0f ? 0.0001f * i : stops[i];
            if (i > 0 && fr[i] <= fr[i - 1]) {
                fr[i] = fr[i - 1] + 0.0001f;
            }
        }
        return new RadialGradientPaint(new Point2D.Float(cx, cy), Math.max(r, 0.01f),
                fr, cs, MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }
}
