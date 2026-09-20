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
    // Замер по эталону: и основные, и промежуточные деления идут от 0.79 до
    // 0.885 радиуса. Промежуточные были вчетверо короче — отсюда и ощущение
    // пустой шкалы.
    private static final float TICK_OUT    = 0.885f;
    private static final float TICK_MAJ_IN = 0.790f;
    private static final float TICK_MIN_IN = 0.800f;
    private static final float LABEL_R     = 0.700f;   //  90 px

    /** Радиальный профиль яркости кольца: внутри тускло, пик у 0.94, к краю спад. */
    private static final float[] BEZEL_BANDS = {0.30f, 0.72f, 1.00f, 0.75f, 0.40f, 0.31f};

    /** Циферблат подсвечен до самого безеля: ореол на эталоне живёт на 0.90. */
    private static final float FACE_OUT = 0.950f;

    /*
     * Красная зона. Замер по эталону (30-й процентиль в секторе 3..30°,
     * то есть между штрихами) вдоль радиуса:
     *
     *   r/R   0.66  0.70  0.74  0.78  0.82  0.86  0.88  0.90  0.92  0.94
     *   R     9    12    16    24    35    50    77   125    32    39
     *
     * Это не плашка под делениями, а длинный внутренний подсвет с резким
     * пиком снаружи от шкалы (0.88..0.92) и тёмным зазором перед безелем.
     * Раньше зона рисовалась ровно наоборот — плотной полосой под штрихами.
     */
    private static final float[] RED_AT = {
            0.640f, 0.700f, 0.760f, 0.800f, 0.840f, 0.870f,
            0.888f, 0.902f, 0.916f, 0.926f, 0.944f,
    };
    private static final int[] RED_ALPHA = {
            6, 14, 24, 34, 52, 76, 132, 220, 196, 44, 30,
    };

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
        draw(c, t, cx, cy, r, ch, min, max, decimals, majorTicks, labelEvery,
                redlineFrom, caption, unit, tickScale, tickDecimals,
                showValue, emptyNote, needleAt, 2);
    }

    /**
     * {@code minorPerMajor} — на сколько частей делится основной интервал
     * шкалы. На эталоне это не константа: у тахометра шаг штрихов 15.5°, у
     * наддува — 4.6°, у давления топлива — 6°. Одно деление посередине, как
     * было раньше, давало пустую, «дешёвую» шкалу на всех приборах, кроме
     * тахометра.
     */
    public static void draw(Canvas c, Theme t, float cx, float cy, float r,
                            Channel ch, float min, float max, int decimals,
                            int majorTicks, int labelEvery, float redlineFrom,
                            String caption, String unit,
                            float tickScale, int tickDecimals,
                            boolean showValue, String emptyNote, float needleAt,
                            int minorPerMajor) {

        int save = c.save();
        c.translate(cx, cy);

        // 1. Безель, тёмное кольцо, циферблат.
        drawRings(c, t, r);

        dialBody(c, t, r, ch, min, max, decimals, majorTicks, labelEvery,
                redlineFrom, caption, unit, tickScale, tickDecimals,
                showValue, emptyNote, needleAt, minorPerMajor);

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
        // Циферблат заливается ДО безеля и на всю глубину под него: подсвет
        // шкалы на эталоне лежит на 0.90 радиуса, то есть снаружи от полосы
        // делений. Обрезать заливку на 0.883 радиуса значило бы срезать ровно ту
        // часть ореола, ради которой он и заводится.
        c.drawCircle(0f, 0f, r * FACE_OUT, t.fill(Theme.RING_DARK));
        c.drawCircle(0f, 0f, r * FACE_OUT, t.dial(r));
        if (Sprites.hasBezel()) {
            // Кольцо берётся с эталона: полированный металл с бликом и тенью
            // примитивами не набирается. Растр кладётся в свой размер, при
            // штатной геометрии окна — почти один к одному.
            Sprites.bezel(c, 0f, 0f, r);
        } else {
            drawBezelRings(c, t, r);
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
                                 float needleAt, int minorPerMajor) {

        // 2. Красная зона. На эталоне это не плашка, а градиент: у обода
        //    насыщенный (186,62,49), внутрь гаснет до сероватого. Набираем
        //    несколькими дугами с растущей к ободу непрозрачностью.
        if (!Float.isNaN(redlineFrom) && redlineFrom > min && redlineFrom < max) {
            float f0 = (redlineFrom - min) / (max - min);
            float from = ARC_START + ARC_SWEEP * f0;
            float sweep = ARC_START + ARC_SWEEP - from;
            for (int i = 0; i < RED_AT.length - 1; i++) {
                float rr = (RED_AT[i] + RED_AT[i + 1]) * 0.5f * r;
                float bw = (RED_AT[i + 1] - RED_AT[i]) * r + 0.8f;
                int alpha = (RED_ALPHA[i] + RED_ALPHA[i + 1]) / 2;
                t.rect.set(-rr, -rr, rr, rr);
                c.drawArc(t.rect, from, sweep, false,
                        t.stroke((alpha << 24) | (Theme.RED_ZONE & 0xFFFFFF), bw));
            }
        }

        // 3. Промежуточные деления. subdiv — на сколько частей делится
        //    основной интервал; чем мельче шаг, тем тоньше и короче штрих,
        //    иначе обод сливается в сплошную полосу.
        int subdiv = minorPerMajor < 2 ? 2 : minorPerMajor;
        boolean fine = subdiv > 3;
        float minWidth = fine ? 0.011f : 0.024f;
        float minIn = fine ? 0.852f : TICK_MIN_IN;
        for (int i = 1; i < majorTicks * subdiv; i++) {
            if (i % subdiv == 0) {
                continue;   // здесь стоит основное деление
            }
            float f = i / (float) (majorTicks * subdiv);
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            tickGlow(c, t, cos, sin, minIn, r, minWidth * 2.2f, fine ? 12 : 26);
            c.drawLine(cos * r * minIn, sin * r * minIn,
                    cos * r * TICK_OUT, sin * r * TICK_OUT,
                    t.stroke(fine ? 0x9ECFC8E0 : Theme.TICK_MINOR, r * minWidth));
        }

        // 4. Основные деления и подписи шкалы.
        // Ширина подписи решает, где она помещается: «8» и «-1.0» — разные
        // задачи. На эталоне у наддува цифры мельче и лежат ближе к центру
        // (0.65 радиуса против 0.70 у тахометра); без этого «-1.0» и «2.0»
        // наезжали на штрихи.
        int widest = 1;
        for (int i = 0; i <= majorTicks; i += (labelEvery > 0 ? labelEvery : 1)) {
            String probe = Channel.format(
                    (min + (max - min) * i / majorTicks) / tickScale, tickDecimals);
            if (probe.length() > widest) {
                widest = probe.length();
            }
        }
        float labelSize = widest >= 4 ? r * 0.122f : (widest == 3 ? r * 0.132f : r * 0.150f);
        float labelR = widest >= 4 ? 0.624f : (widest == 3 ? 0.652f : LABEL_R);
        for (int i = 0; i <= majorTicks; i++) {
            float f = (float) i / majorTicks;
            float value = min + (max - min) * f;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            // Деления и цифры в красной зоне на эталоне остаются светлыми —
            // красным там окрашена только полоса под ними.
            tickGlow(c, t, cos, sin, TICK_MAJ_IN, r, 0.052f, 34);
            c.drawLine(cos * r * TICK_MAJ_IN, sin * r * TICK_MAJ_IN,
                    cos * r * TICK_OUT, sin * r * TICK_OUT,
                    t.stroke(Theme.TICK, r * 0.030f));
            if (labelEvery > 0 && i % labelEvery == 0) {
                String label = Channel.format(value / tickScale, tickDecimals);
                float lx = cos * r * labelR;
                float ly = sin * r * labelR + labelSize * 0.36f;
                // Цифры на эталоне не вырезаны из черноты — под ними мягкое
                // свечение. Обводка полупрозрачным лавандовым даёт его без
                // размытия, которого на Android 2.3 всё равно нет.
                c.drawText(label, lx, ly,
                        t.textGlow(label, labelSize, r * 0.040f, Paint.Align.CENTER));
                c.drawText(label, lx, ly,
                        t.textFor(label, Theme.TICK, labelSize,
                                Paint.Align.CENTER, false));
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
            // Под стрелкой — её собственный свет: два широких полупрозрачных
            // прохода тем же цветом. На эталоне стрелка «горит», а не лежит.
            c.drawPath(np, t.stroke((hot ? 0x40000000 : 0x38000000)
                    | ((hot ? Theme.RED : Theme.NEEDLE) & 0xFFFFFF), r * 0.030f));
            c.drawPath(np, t.stroke((hot ? 0x60000000 : 0x58000000)
                    | ((hot ? Theme.RED : Theme.NEEDLE) & 0xFFFFFF), r * 0.014f));
            c.drawPath(np, t.fill(hot ? Theme.RED : Theme.NEEDLE));
        }

        // Ступица. На эталоне это не «мишень» из колец, а один матовый диск
        // чуть светлее циферблата с мягким тёмным ободком; поверх него идёт
        // тёмный противовес стрелки. Светлого ядра там нет вовсе — прежние
        // три концентрических круга были домыслом.
        // Яркость снята с эталона: внутри ступицы (12,17,23), по краю провал
        // почти в ноль. Светлее делать нельзя — диск сразу «всплывает».
        c.drawCircle(0f, 0f, r * 0.152f, t.fill(0x22000000));
        c.drawCircle(0f, 0f, r * 0.140f, t.fill(0xFF0C1117));
        c.drawCircle(0f, 0f, r * 0.140f, t.stroke(0x66000000, r * 0.014f));
        if (has) {
            float f = Float.isNaN(needleAt)
                    ? (ch.getValue() - min) / (max - min) : needleAt;
            f = f < 0f ? 0f : (f > 1f ? 1f : f);
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * r * 0.10f, sin * r * 0.10f,
                    -cos * r * 0.215f, -sin * r * 0.215f,
                    t.stroke(0xFF161B22, r * 0.052f));
            c.drawLine(cos * r * 0.10f, sin * r * 0.10f,
                    -cos * r * 0.215f, -sin * r * 0.215f,
                    t.stroke(0x22000000, r * 0.020f));
        }

        // 6. Название, единица, цифровое значение.
        // Базовые линии с эталона: RPM на 137, x1000 на 152 при cy = 181.
        c.drawText(caption, 0f, -r * 0.344f,
                t.textOutline(caption, r * 0.125f, r * 0.055f, Paint.Align.CENTER, false));
        c.drawText(caption, 0f, -r * 0.344f,
                t.textFor(caption, Theme.WHITE, r * 0.125f, Paint.Align.CENTER, false));
        if (unit != null) {
            c.drawText(unit, 0f, -r * 0.227f,
                    t.textOutline(unit, r * 0.086f, r * 0.045f, Paint.Align.CENTER, false));
            c.drawText(unit, 0f, -r * 0.227f,
                    t.textFor(unit, Theme.LABEL, r * 0.086f, Paint.Align.CENTER, false));
        }
        if (showValue && has) {
            c.drawText(ch.text(decimals), 0f, r * 0.46f,
                    t.textOutline(ch.text(decimals), r * 0.215f, r * 0.060f,
                            Paint.Align.CENTER, false));
            c.drawText(ch.text(decimals), 0f, r * 0.46f,
                    t.textFor(ch.text(decimals), Theme.WHITE, r * 0.215f,
                            Paint.Align.CENTER, false));
        } else if (!has && emptyNote != null) {
            // Канал не логируется — говорим об этом, а не рисуем число.
            c.drawText("—", 0f, r * 0.44f,
                    t.text(Theme.VALUE_DIM, r * 0.22f, Paint.Align.CENTER, false));
            c.drawText(emptyNote, 0f, r * 0.62f,
                    t.text(Theme.VALUE_DIM, r * 0.088f, Paint.Align.CENTER, false));
        }
    }

    /**
     * Ореол под делением: широкий полупрозрачный штрих того же направления.
     * На эталоне свет от штрихов растекается по циферблату, и именно он
     * создаёт лавандовое кольцо под шкалой.
     */
    private static void tickGlow(Canvas c, Theme t, float cos, float sin,
                                 float inner, float r, float width, int alpha) {
        c.drawLine(cos * r * (inner + 0.01f), sin * r * (inner + 0.01f),
                cos * r * (TICK_OUT - 0.01f), sin * r * (TICK_OUT - 0.01f),
                t.stroke((alpha << 24) | (Theme.TICK & 0xFFFFFF), r * width));
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
