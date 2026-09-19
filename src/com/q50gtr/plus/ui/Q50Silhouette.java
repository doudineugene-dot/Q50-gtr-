package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Top-down outline of a Q50, drawn as a Path so the chassis page has a real
 * car under its four corner readouts instead of a bitmap the head unit would
 * have to decode.
 *
 * Coordinates are normalised to the bounding box passed in, so the drawing
 * scales with whatever the chassis page gives it.
 */
public final class Q50Silhouette {

    private Q50Silhouette() {
    }

    /** Draws the car to fill the given box. */
    public static void draw(Canvas c, Theme t, float x, float y, float w, float h) {
        Path p = t.path;
        p.reset();

        float cx = x + w / 2f;
        float nose = y;
        float tail = y + h;
        float halfBody = w * 0.30f;
        float halfHip = w * 0.34f;

        // Body: nose, along the right flank, around the tail, back up the left.
        p.moveTo(cx, nose);
        p.cubicTo(cx + halfBody * 0.75f, nose + h * 0.010f,
                cx + halfHip, nose + h * 0.10f,
                cx + halfHip, nose + h * 0.26f);
        p.cubicTo(cx + halfHip * 1.02f, nose + h * 0.48f,
                cx + halfHip, nose + h * 0.66f,
                cx + halfHip * 0.98f, nose + h * 0.82f);
        p.cubicTo(cx + halfHip * 0.92f, tail - h * 0.05f,
                cx + halfBody * 0.70f, tail,
                cx, tail);
        p.cubicTo(cx - halfBody * 0.70f, tail,
                cx - halfHip * 0.92f, tail - h * 0.05f,
                cx - halfHip * 0.98f, nose + h * 0.82f);
        p.cubicTo(cx - halfHip, nose + h * 0.66f,
                cx - halfHip * 1.02f, nose + h * 0.48f,
                cx - halfHip, nose + h * 0.26f);
        p.cubicTo(cx - halfHip, nose + h * 0.10f,
                cx - halfBody * 0.75f, nose + h * 0.010f,
                cx, nose);
        p.close();

        c.drawPath(p, t.fill(Theme.PANEL_TOP));
        c.drawPath(p, t.stroke(Theme.SILVER, 1.6f));

        // Greenhouse: windscreen, roof, rear screen.
        p.reset();
        float roofTop = y + h * 0.34f;
        float roofBottom = y + h * 0.66f;
        float roofHalf = w * 0.21f;
        p.moveTo(cx - roofHalf * 0.72f, roofTop);
        p.lineTo(cx + roofHalf * 0.72f, roofTop);
        p.lineTo(cx + roofHalf, roofBottom);
        p.lineTo(cx - roofHalf, roofBottom);
        p.close();
        c.drawPath(p, t.stroke(Theme.EDGE, 1.2f));

        c.drawLine(cx - roofHalf * 0.86f, y + h * 0.42f, cx + roofHalf * 0.86f, y + h * 0.42f,
                t.stroke(Theme.EDGE, 1f));
        c.drawLine(cx - roofHalf * 0.95f, y + h * 0.56f, cx + roofHalf * 0.95f, y + h * 0.56f,
                t.stroke(Theme.EDGE, 1f));

        // A thin accent spine, the one piece of colour on the car.
        c.drawLine(cx, y + h * 0.06f, cx, y + h * 0.30f, t.stroke(Theme.ACCENT_DEEP, 1.2f));

        // Wheels.
        float wheelW = w * 0.075f;
        float wheelH = h * 0.135f;
        float axleFront = y + h * 0.235f;
        float axleRear = y + h * 0.745f;
        float track = w * 0.335f;

        wheel(c, t, cx - track, axleFront, wheelW, wheelH);
        wheel(c, t, cx + track, axleFront, wheelW, wheelH);
        wheel(c, t, cx - track, axleRear, wheelW, wheelH);
        wheel(c, t, cx + track, axleRear, wheelW, wheelH);
    }

    private static void wheel(Canvas c, Theme t, float cx, float cy, float w, float h) {
        t.rect2.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        c.drawRoundRect(t.rect2, w * 0.35f, w * 0.35f, t.fill(Theme.BG));
        c.drawRoundRect(t.rect2, w * 0.35f, w * 0.35f, t.stroke(Theme.SILVER, 1.4f));
    }

    /** Small caption under the car. */
    public static void caption(Canvas c, Theme t, float cx, float baseline, String s) {
        c.drawText(s, cx, baseline, t.text(Theme.DIM, 11f, Paint.Align.CENTER, false));
    }
}
