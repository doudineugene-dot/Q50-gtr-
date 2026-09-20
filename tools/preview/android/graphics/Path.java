package android.graphics;

import java.awt.geom.Path2D;

public class Path {
    public final Path2D.Float p = new Path2D.Float();
    private float cx, cy;

    public void reset() { p.reset(); cx = 0f; cy = 0f; }
    public void moveTo(float x, float y) { p.moveTo(x, y); cx = x; cy = y; }
    public void lineTo(float x, float y) { p.lineTo(x, y); cx = x; cy = y; }
    public void quadTo(float x1, float y1, float x2, float y2) {
        p.quadTo(x1, y1, x2, y2); cx = x2; cy = y2;
    }
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        quadTo(cx + dx1, cy + dy1, cx + dx2, cy + dy2);
    }
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        p.curveTo(x1, y1, x2, y2, x3, y3); cx = x3; cy = y3;
    }
    public void close() { p.closePath(); }
}
