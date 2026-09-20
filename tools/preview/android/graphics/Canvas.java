package android.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Canvas поверх Java2D — ровно те методы, которыми пользуется панель. */
public class Canvas {

    private final Graphics2D g;
    private final List<AffineTransform> xforms = new ArrayList<AffineTransform>();
    private final List<java.awt.Shape> clips = new ArrayList<java.awt.Shape>();

    public Canvas(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public int save() {
        xforms.add(g.getTransform());
        clips.add(g.getClip());
        return xforms.size();
    }

    public void restore() {
        int n = xforms.size() - 1;
        if (n >= 0) {
            g.setTransform(xforms.remove(n));
            g.setClip(clips.remove(n));
        }
    }

    public void restoreToCount(int count) {
        while (xforms.size() >= count && !xforms.isEmpty()) {
            restore();
        }
    }

    public void translate(float dx, float dy) { g.translate(dx, dy); }
    public void scale(float sx, float sy) { g.scale(sx, sy); }

    public void clipRect(float l, float t, float r, float b) {
        g.clip(new Rectangle2D.Float(l, t, r - l, b - t));
    }

    public void drawColor(int color) {
        g.setPaint(new Color(color, true));
        g.fill(new Rectangle2D.Float(-10000f, -10000f, 20000f, 20000f));
    }

    private void apply(Paint p) {
        Shader s = p.getShader();
        java.awt.Paint awt = s != null ? s.awt() : null;
        g.setPaint(awt != null ? awt : new Color(p.getColor(), true));
        int cap = p.getStrokeCap() == Paint.Cap.ROUND ? BasicStroke.CAP_ROUND
                : (p.getStrokeCap() == Paint.Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT);
        g.setStroke(new BasicStroke(Math.max(p.getStrokeWidth(), 0.01f), cap, BasicStroke.JOIN_MITER));
    }

    private void paintShape(java.awt.Shape shape, Paint p) {
        apply(p);
        if (p.getStyle() == Paint.Style.STROKE) {
            g.draw(shape);
        } else {
            g.fill(shape);
            if (p.getStyle() == Paint.Style.FILL_AND_STROKE) {
                g.draw(shape);
            }
        }
    }

    public void drawCircle(float cx, float cy, float r, Paint p) {
        paintShape(new Ellipse2D.Float(cx - r, cy - r, r * 2f, r * 2f), p);
    }

    public void drawLine(float x1, float y1, float x2, float y2, Paint p) {
        apply(p);
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    public void drawRect(RectF r, Paint p) {
        paintShape(new Rectangle2D.Float(r.left, r.top, r.width(), r.height()), p);
    }

    public void drawRoundRect(RectF r, float rx, float ry, Paint p) {
        paintShape(new RoundRectangle2D.Float(r.left, r.top, r.width(), r.height(),
                rx * 2f, ry * 2f), p);
    }

    public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint p) {
        // Android отсчитывает углы по часовой стрелке, Java2D — против.
        Arc2D.Float arc = new Arc2D.Float(oval.left, oval.top, oval.width(), oval.height(),
                -startAngle, -sweepAngle, useCenter ? Arc2D.PIE : Arc2D.OPEN);
        apply(p);
        if (p.getStyle() == Paint.Style.STROKE) {
            g.draw(arc);
        } else {
            g.fill(arc);
        }
    }

    public void drawPath(Path path, Paint p) {
        paintShape(path.p, p);
    }

    public void drawText(String text, float x, float y, Paint p) {
        apply(p);
        g.setFont(p.awtFont());
        float w = (float) g.getFontMetrics().getStringBounds(text, g).getWidth();
        float tx = x;
        if (p.getTextAlign() == Paint.Align.CENTER) {
            tx = x - w / 2f;
        } else if (p.getTextAlign() == Paint.Align.RIGHT) {
            tx = x - w;
        }
        g.drawString(text, tx, y);
    }
}
