package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.q50gtr.plus.data.Channel;

/**
 * Q50 сзади и четыре пневмостойки вокруг — так, как это показано на
 * приборной панели: машина по центру, давления по углам.
 */
public final class Q50Rear {

    private Q50Rear() {
    }

    /** Кузов вписывается в прямоугольник (x, y, w, h). */
    public static void car(Canvas c, Theme t, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        Path p = t.path;

        // Крыша: узкая сверху, расширяется к плечам — силуэт седана сзади.
        float roofTop = y + h * 0.06f;
        float glassBottom = y + h * 0.40f;
        p.reset();
        p.moveTo(cx - w * 0.21f, roofTop);
        p.quadTo(cx, roofTop - h * 0.03f, cx + w * 0.21f, roofTop);
        p.quadTo(cx + w * 0.30f, y + h * 0.22f, cx + w * 0.33f, glassBottom);
        p.lineTo(cx - w * 0.33f, glassBottom);
        p.quadTo(cx - w * 0.30f, y + h * 0.22f, cx - w * 0.21f, roofTop);
        p.close();
        c.drawPath(p, t.fill(0xFF1A2432));
        c.drawPath(p, t.stroke(0xFF4C5C72, 1.3f));

        // Заднее стекло.
        p.reset();
        p.moveTo(cx - w * 0.18f, roofTop + h * 0.04f);
        p.lineTo(cx + w * 0.18f, roofTop + h * 0.04f);
        p.lineTo(cx + w * 0.27f, glassBottom - h * 0.03f);
        p.lineTo(cx - w * 0.27f, glassBottom - h * 0.03f);
        p.close();
        c.drawPath(p, t.fill(0xFF0C141F));

        // Кузов с плечами над арками.
        p.reset();
        float halfB = w * 0.46f;
        p.moveTo(cx - w * 0.33f, glassBottom);
        p.lineTo(cx + w * 0.33f, glassBottom);
        p.quadTo(cx + halfB * 0.92f, y + h * 0.46f, cx + halfB, y + h * 0.60f);
        p.lineTo(cx + halfB, y + h * 0.86f);
        p.quadTo(cx + halfB, y + h * 0.94f, cx + halfB - w * 0.06f, y + h * 0.94f);
        p.lineTo(cx - halfB + w * 0.06f, y + h * 0.94f);
        p.quadTo(cx - halfB, y + h * 0.94f, cx - halfB, y + h * 0.86f);
        p.lineTo(cx - halfB, y + h * 0.60f);
        p.quadTo(cx - halfB * 0.92f, y + h * 0.46f, cx - w * 0.33f, glassBottom);
        p.close();
        c.drawPath(p, t.fill(0xFF16202E));
        c.drawPath(p, t.stroke(0xFF56687F, 1.5f));

        // Фонари — узкие, вытянутые к бокам.
        float ly = y + h * 0.56f;
        float lh = h * 0.085f;
        t.rect.set(cx - halfB + w * 0.02f, ly, cx - w * 0.19f, ly + lh);
        c.drawRoundRect(t.rect, lh * 0.45f, lh * 0.45f, t.fill(0xFFC03028));
        t.rect.set(cx + w * 0.19f, ly, cx + halfB - w * 0.02f, ly + lh);
        c.drawRoundRect(t.rect, lh * 0.45f, lh * 0.45f, t.fill(0xFFC03028));

        // Эмблема, номер и выхлоп.
        c.drawCircle(cx, y + h * 0.60f, w * 0.042f, t.stroke(0xFF97A2B2, 1.2f));
        c.drawLine(cx - w * 0.030f, y + h * 0.60f, cx + w * 0.030f, y + h * 0.60f,
                t.stroke(0xFF97A2B2, 1.2f));
        t.rect.set(cx - w * 0.115f, y + h * 0.72f, cx + w * 0.115f, y + h * 0.83f);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF0E1622));
        c.drawRoundRect(t.rect, 2f, 2f, t.stroke(0xFF5E6E85, 1.1f));
        t.rect.set(cx - halfB * 0.66f, y + h * 0.95f, cx - halfB * 0.40f, y + h * 0.99f);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF6A798F));
        t.rect.set(cx + halfB * 0.40f, y + h * 0.95f, cx + halfB * 0.66f, y + h * 0.99f);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF6A798F));
    }

    /**
     * Пневмостойка с давлением. {@code labelLeft} разводит подпись по ту
     * сторону стойки, где для неё есть место.
     */
    public static void strut(Canvas c, Theme t, float cx, float cy, float h,
                             Channel ch, boolean labelLeft) {
        float top = cy - h / 2f;
        float bottom = cy + h / 2f;
        float w = h * 0.30f;

        // Шток и опоры.
        c.drawLine(cx, top, cx, top + h * 0.16f, t.stroke(0xFF98A2B2, 3f));
        t.rect.set(cx - w * 0.62f, top, cx + w * 0.62f, top + h * 0.06f);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF8A94A4));
        t.rect.set(cx - w * 0.62f, bottom - h * 0.06f, cx + w * 0.62f, bottom);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF8A94A4));

        // Витки пневмобаллона.
        int coils = 5;
        float cTop = top + h * 0.18f;
        float cBottom = bottom - h * 0.10f;
        float step = (cBottom - cTop) / coils;
        for (int i = 0; i < coils; i++) {
            float yy = cTop + step * i;
            t.rect.set(cx - w / 2f, yy, cx + w / 2f, yy + step * 1.35f);
            c.drawArc(t.rect, 0f, 180f, false, t.stroke(0xFF7E8A9B, 2.4f));
        }

        // Давление подписывается рядом со стойкой.
        String text = ch.hasValue() ? ch.text(1) : "—";
        Paint p = t.text(ch.hasValue() ? Theme.WHITE : Theme.VALUE_DIM, 21f,
                labelLeft ? Paint.Align.RIGHT : Paint.Align.LEFT, true);
        float tx = labelLeft ? cx - w * 0.80f : cx + w * 0.80f;
        c.drawText(text, tx, cy + 8f, p);
    }
}
