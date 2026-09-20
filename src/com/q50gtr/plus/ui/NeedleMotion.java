package com.q50gtr.plus.ui;

import com.q50gtr.plus.data.Channel;

/**
 * Демпфирование стрелок.
 *
 * Настоящая приборная панель не дёргает стрелкой на каждый кадр: у механизма
 * есть инерция, и именно она читается как «дорогая» стрелка. Здесь то же
 * самое — экспоненциальное сглаживание с постоянной времени около 120 мс.
 *
 * Сглаживается ТОЛЬКО положение стрелки. Цифровые значения выводятся как
 * пришли: показывать водителю подогнанное число нельзя, а стрелка и так не
 * инструмент точного отсчёта — для этого рядом стоит цифра.
 *
 * Если канал пропал или устарел, накопленное состояние сбрасывается, чтобы
 * при возврате связи стрелка не приползала из старого положения.
 */
public final class NeedleMotion {

    /** За столько миллисекунд стрелка проходит ~63% пути до цели. */
    private static final float TAU_MS = 120f;
    /** Ближе этой доли шкалы доводим сразу: иначе стрелка вечно «доползает». */
    private static final float SNAP = 0.0015f;

    private float position;
    private boolean primed;
    private long lastMs;

    /** Положение стрелки в долях шкалы, 0..1. */
    public float update(Channel ch, float min, float max, long nowMs) {
        if (!ch.hasValue() || max == min) {
            primed = false;
            return Float.NaN;
        }

        float target = (ch.getValue() - min) / (max - min);
        target = target < 0f ? 0f : (target > 1f ? 1f : target);

        if (!primed) {
            primed = true;
            position = target;
            lastMs = nowMs;
            return position;
        }

        long dt = nowMs - lastMs;
        lastMs = nowMs;
        if (dt <= 0L) {
            return position;
        }
        if (dt > 1000L) {
            // Большой разрыв: приложение было свёрнуто. Догонять нечего.
            position = target;
            return position;
        }

        float k = 1f - (float) Math.exp(-dt / TAU_MS);
        position += (target - position) * k;
        if (Math.abs(target - position) < SNAP) {
            position = target;
        }
        return position;
    }

    public void reset() {
        primed = false;
    }
}
