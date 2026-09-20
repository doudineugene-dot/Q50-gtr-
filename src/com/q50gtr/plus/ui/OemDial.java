package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Круглый прибор штатного кластера Q50.
 *
 * Углы шкалы и доли радиуса измерены по MASTER: радиальный профиль яркости
 * вдоль 360 лучей даёт кольца (безель, тёмное кольцо, полоса делений, полоса
 * подписей), а угловой профиль в полосе делений — сами деления. См.
 * docs/UI-MASTER-SPEC.md, п.5.
 *
 * Стрелка считается строго как (value − min) / (max − min): шкала обязана
 * соответствовать числу, декоративных сдвигов нет.
 */
public final class OemDial {

    /** Первое деление на эталоне — 146.2°, последнее — 393.7°. */
    public static final float ARC_START = 146.2f;
    public static final float ARC_SWEEP = 247.5f;

    /* Доли внешнего радиуса безеля — из радиального профиля эталона:
     * 0.95..1.00 безель, 0.885..0.945 тёмное кольцо, 0.80..0.875 деления,
     * ~0.70 подписи, дальше циферблат. */
    private static final float BEZEL_IN    = 0.914f;   // 117 px при R = 128
    private static final float RING_IN     = 0.883f;   // 113 px
    // Замер по эталону: и основные, и промежуточные деления идут от 0.79 до
    // 0.885 радиуса. Промежуточные были вчетверо короче — отсюда и ощущение
    // пустой шкалы.
    private static final float TICK_OUT    = 0.885f;
    private static final float TICK_MAJ_IN = 0.790f;
    private static final float TICK_MIN_IN = 0.800f;
    private static final float LABEL_R     = 0.700f;   //  90 px

    /** Радиальный профиль яркости кольца: внутри тускло, пик у 0.94, к краю спад. */
    private static final float[] BEZEL_BANDS = {0.30f, 0.72f, 1.00f, 0.75f, 0.40f, 0.31f};

    /** Полос в градиенте красной зоны. */
    private static final int RED_BANDS = 5;

    /** Колец в ореоле вокруг прибора. */
    private static final int GLOW_STEPS = 7;

    /** Кончик и хвост стрелки в долях радиуса — по замерам эталона. */
    private static final float NEEDLE_TIP = 0.780f;
    private static final float NEEDLE_TAIL = 0.02f;

    private OemDial() {
    }

    public static void draw(Canvas c, Theme t, float cx, float cy, float r,
                            Channel ch, float min, float max, int decimals,
                            int majorTicks, int labelEvery, float redlineFrom,
                            String caption, String unit,
                            float tickScale, int tickDecimals,
                            boolean showValue, String emptyNote) {
        draw(c, t, cx, cy, r, ch, min, max, decimals, majorTicks, labelEvery,
                redlineFrom, caption, unit, tickScale, tickDecimals,
                showValue, emptyNote, Float.NaN);
    }

    /**
     * {@code needleAt} — положение стрелки в долях шкалы, уже сглаженное
     * {@link NeedleMotion}. NaN означает «считать прямо из значения».
     */
    public static void draw(Canvas c, Theme t, float cx, float cy, float r,
                            Channel ch, float min, float max, int decimals,
                            int majorTicks, int labelEvery, float redlineFrom,
                            String caption, String unit,
                            float tickScale, int tickDecimals,
                            boolean showValue, String emptyNote, float needleAt) {

        int save = c.save();
        c.translate(cx, cy);

        // 1. Безель, тёмное кольцо, циферблат.
        drawRings(c, t, r);

        dialBody(c, t, r, ch, min, max, decimals, majorTicks, labelEvery,
                redlineFrom, caption, unit, tickScale, tickDecimals,
                showValue, emptyNote, needleAt);

        c.restoreToCount(save);
    }

    private static void drawRings(Canvas c, Theme t, float r) {
        // Мягкий ореол вокруг прибора: на эталоне кольцо не висит на плоском
        // чёрном, под ним есть подсвет. Несколько полупрозрачных колец дают
        // его дешевле, чем RadialGradient на каждый кадр.
        for (int i = 0; i < GLOW_STEPS; i++) {
            float k = (i + 1) / (float) GLOW_STEPS;
            float gr = r * (1f + 0.09f * k);
            int alpha = (int) (12f * (1f - k) * (1f - k));
            if (alpha <= 0) {
                continue;
            }
            c.drawCircle(0f, 0f, gr, t.fill((alpha << 24) | 0x5A6A9A));
        }

        // Кольцо рисуется дугами, а не растром: растр пришлось бы
        // масштабировать под фактический размер прибора, а это ровно то мыло,
        // из-за которого от полноэкранного фона и отказались.
        if (Sprites.hasBezel()) {
            // Кольцо берётся с эталона: полированный металл с бликом и тенью
            // примитивами не набирается. Растр кладётся в свой размер, при
            // штатной геометрии окна — почти один к одному.
            c.drawCircle(0f, 0f, r * BEZEL_IN, t.fill(Theme.RING_DARK));
            c.drawCircle(0f, 0f, r * RING_IN, t.dial(r));
            Sprites.bezel(c, 0f, 0f, r);
        } else {
            drawBezelRings(c, t, r);
            c.drawCircle(0f, 0f, r * BEZEL_IN, t.fill(Theme.RING_DARK));
            c.drawCircle(0f, 0f, r * RING_IN, t.dial(r));
        }
    }

    /** Запасной безель, если растр недоступен: кольцо из дуг по профилю. */
    private static void drawBezelRings(Canvas c, Theme t, float r) {
        for (int band = 0; band < BEZEL_BANDS.length; band++) {
            float f0 = BEZEL_IN + (1f - BEZEL_IN) * band / BEZEL_BANDS.length;
            float f1 = BEZEL_IN + (1f - BEZEL_IN) * (band + 1) / BEZEL_BANDS.length;
            float br = r * (f0 + f1) * 0.5f;
            float bw = r * (f1 - f0) + 0.6f;
            t.rect.set(-br, -br, br, br);
            for (int i = 0; i < 36; i++) {
                float a0 = i * 10f;
                c.drawArc(t.rect, a0 - 0.8f, 11.6f, false,
                        t.stroke(Theme.bezelAt(a0 + 5f, BEZEL_BANDS[band]), bw));
            }
        }
    }

    private static void dialBody(Canvas c, Theme t, float r,
                                 Channel ch, float min, float max, int decimals,
                                 int majorTicks, int labelEvery, float redlineFrom,
                                 String caption, String unit,
                                 float tickScale, int tickDecimals,
                                 boolean showValue, String emptyNote,
                                 float needleAt) {

        // 2. Красная зона. На эталоне это не плашка, а градиент: у обода
        //    насыщенный (186,62,49), внутрь гаснет до сероватого. Набираем
        //    несколькими дугами с растущей к ободу непрозрачностью.
        if (!Float.isNaN(redlineFrom) && redlineFrom > min && redlineFrom < max) {
            float f0 = (redlineFrom - min) / (max - min);
            float from = ARC_START + ARC_SWEEP * f0;
            float sweep = ARC_START + ARC_SWEEP - from;
            float step = (TICK_OUT - TICK_MAJ_IN) / RED_BANDS;
            for (int i = 0; i < RED_BANDS; i++) {
                float inner = TICK_MAJ_IN + step * i;
                float rr = (inner + step * 0.5f) * r;
                int alpha = 70 + (int) (170f * i / (float) (RED_BANDS - 1));
                t.rect.set(-rr, -rr, rr, rr);
                c.drawArc(t.rect, from, sweep, false,
                        t.stroke((alpha << 24) | (Theme.RED_ZONE & 0xFFFFFF),
                                step * r + 0.8f));
            }
        }

        // 3. Промежуточные деления — посередине между основными.
        for (int i = 0; i < majorTicks; i++) {
            float f = (i + 0.5f) / majorTicks;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * r * TICK_MIN_IN, sin * r * TICK_MIN_IN,
                    cos * r * TICK_OUT, sin * r * TICK_OUT,
                    t.stroke(Theme.TICK_MINOR, r * 0.024f));
        }

        // 4. Основные деления и подписи шкалы.
        Paint tickText = t.text(Theme.TICK, r * 0.142f, Paint.Align.CENTER, false);
        for (int i = 0; i <= majorTicks; i++) {
            float f = (float) i / majorTicks;
            float value = min + (max - min) * f;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            // Деления и цифры в красной зоне на эталоне остаются светлыми —
            // красным там окрашена только полоса под ними.
            c.drawLine(cos * r * TICK_MAJ_IN, sin * r * TICK_MAJ_IN,
                    cos * r * TICK_OUT, sin * r * TICK_OUT,
                    t.stroke(Theme.TICK, r * 0.030f));
            if (labelEvery > 0 && i % labelEvery == 0) {
                tickText.setColor(Theme.TICK);
                c.drawText(Channel.format(value / tickScale, tickDecimals),
                        cos * r * LABEL_R, sin * r * LABEL_R + r * 0.055f, tickText);
            }
        }

        boolean has = ch.hasValue();

        // 5. Стрелка и ступица. Рисуются до подписей, иначе стрелка
        //    перечёркивает название прибора.
        if (has) {
            float f = Float.isNaN(needleAt)
                    ? (ch.getValue() - min) / (max - min) : needleAt;
            f = f < 0f ? 0f : (f > 1f ? 1f : f);
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            boolean hot = !Float.isNaN(redlineFrom) && ch.getValue() >= redlineFrom;
            // Клин, а не треугольник с широким основанием: на эталоне
            // стрелка тонкая — 4..5 px у ступицы и 2 px у кончика.
            float nx = -sin, ny = cos;
            float baseW = r * 0.0125f;
            float tipW = r * 0.0070f;
            android.graphics.Path np = t.path;
            np.reset();
            np.moveTo(cos * r * NEEDLE_TIP + nx * tipW, sin * r * NEEDLE_TIP + ny * tipW);
            np.lineTo(cos * r * NEEDLE_TIP - nx * tipW, sin * r * NEEDLE_TIP - ny * tipW);
            np.lineTo(-cos * r * NEEDLE_TAIL - nx * baseW, -sin * r * NEEDLE_TAIL - ny * baseW);
            np.lineTo(-cos * r * NEEDLE_TAIL + nx * baseW, -sin * r * NEEDLE_TAIL + ny * baseW);
            np.close();
            c.drawPath(np, t.fill(hot ? Theme.RED : Theme.NEEDLE));
        }
        // Ступица тёмная: радиальный профиль эталона даёт 25..33 внутри.
        c.drawCircle(0f, 0f, r * 0.145f, t.fill(Theme.HUB_RIM));
        c.drawCircle(0f, 0f, r * 0.084f, t.fill(0xFF2A313B));
        c.drawCircle(0f, 0f, r * 0.036f, t.fill(0xFF0E131B));

        // 6. Название, единица, цифровое значение.
        // Базовые линии с эталона: RPM на 137, x1000 на 152 при cy = 181.
        c.drawText(caption, 0f, -r * 0.344f,
                t.textOutline(r * 0.125f, r * 0.055f, Paint.Align.CENTER, false));
        c.drawText(caption, 0f, -r * 0.344f,
                t.text(Theme.WHITE, r * 0.125f, Paint.Align.CENTER, false));
        if (unit != null) {
            c.drawText(unit, 0f, -r * 0.227f,
                    t.textOutline(r * 0.086f, r * 0.045f, Paint.Align.CENTER, false));
            c.drawText(unit, 0f, -r * 0.227f,
                    t.text(Theme.LABEL, r * 0.086f, Paint.Align.CENTER, false));
        }
        if (showValue && has) {
            c.drawText(ch.text(decimals), 0f, r * 0.46f,
                    t.textOutline(r * 0.215f, r * 0.060f, Paint.Align.CENTER, false));
            c.drawText(ch.text(decimals), 0f, r * 0.46f,
                    t.text(Theme.WHITE, r * 0.215f, Paint.Align.CENTER, false));
        } else if (!has && emptyNote != null) {
            // Канал не логируется — говорим об этом, а не рисуем число.
            c.drawText("—", 0f, r * 0.44f,
                    t.text(Theme.VALUE_DIM, r * 0.22f, Paint.Align.CENTER, false));
            c.drawText(emptyNote, 0f, r * 0.62f,
                    t.text(Theme.VALUE_DIM, r * 0.088f, Paint.Align.CENTER, false));
        }
    }

    /** Круглый индикатор селектора передач в нижней части циферблата. */
    public static void gearBadge(Canvas c, Theme t, float cx, float cy, float r, String gear) {
        float by = cy + r * 0.50f;
        float br = r * 0.150f;
        c.drawCircle(cx, by, br, t.fill(0xFF0A0F18));
        c.drawCircle(cx, by, br, t.stroke(Theme.LABEL, 1.4f));
        c.drawText(gear, cx, by + br * 0.40f,
                t.text(Theme.WHITE, br * 1.15f, Paint.Align.CENTER, false));
    }
}
