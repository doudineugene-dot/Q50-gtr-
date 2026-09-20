package android.graphics;

import java.awt.Color;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

public class RadialGradient extends Shader {
    private final float cx, cy, r;
    private final int c0, c1;

    public RadialGradient(float cx, float cy, float r, int c0, int c1, TileMode m) {
        this.cx = cx; this.cy = cy; this.r = r; this.c0 = c0; this.c1 = c1;
    }

    @Override
    public java.awt.Paint awt() {
        return new RadialGradientPaint(new Point2D.Float(cx, cy), Math.max(r, 0.01f),
                new float[]{0f, 1f},
                new Color[]{new Color(c0, true), new Color(c1, true)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }
}
