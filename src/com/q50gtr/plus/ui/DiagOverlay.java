package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.InTouchVehicleSource;
import com.q50gtr.plus.data.VehicleData;
import com.q50gtr.plus.data.VehicleProbe;

/**
 * Диагностический оверлей: состояние транспорта и источники ключевых каналов.
 *
 * По умолчанию выключен и ничего не рисует, поэтому утверждённый визуал он не
 * меняет. Включается скрытым жестом — тройное касание левого верхнего угла
 * верхней полосы (см. DashboardView). Отдельная сборка для этого не нужна.
 */
public final class DiagOverlay {

    private DiagOverlay() {
    }

    public static void draw(Canvas c, Theme t, Layout l, DataHub hub,
                            VehicleProbe probe, DisplayInfo display, long nowMs) {
        VehicleData d = hub.getData();

        float w = 340f * l.s;
        float x = l.w - w - 8f * l.s;
        float y = 52f * l.s;
        float lh = 14f * l.s;

        t.rect.set(x, y, x + w, y + lh * 17f + 12f * l.s);
        c.drawRoundRect(t.rect, 4f, 4f, t.fill(0xE0000000));
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(Theme.ACCENT, 1f));

        float ty = y + 16f * l.s;
        if (display != null) {
            ty = line(c, t, x + 8f * l.s, ty, lh, display.summary(), Theme.LABEL);
        }
        ty = line(c, t, x + 8f * l.s, ty, lh,
                "LAYOUT: " + (int) l.w + "x" + (int) l.h + "  s=" + l.s
                        + "  r=" + (int) l.dialR, Theme.LABEL);
        ty = line(c, t, x + 8f * l.s, ty, lh, "SOURCE: " + hub.getSourceLabel(), Theme.ACCENT);
        ty = line(c, t, x + 8f * l.s, ty, lh,
                hub.isLive() ? "STATE:  LIVE" : "STATE:  DEMO (нет связи)",
                hub.isLive() ? 0xFF6BD07A : Theme.WARN);

        if (hub.getInTouch() instanceof InTouchVehicleSource) {
            InTouchVehicleSource s = (InTouchVehicleSource) hub.getInTouch();
            ty = line(c, t, x + 8f * l.s, ty, lh,
                    "INTOUCH: " + (s.isConnected() ? "CONNECTED" : "DISCONNECTED")
                            + "  bound=" + s.getBoundCount(), Theme.WHITE);
            long age = s.getLastFrameMs() == 0 ? -1 : nowMs - s.getLastFrameMs();
            ty = line(c, t, x + 8f * l.s, ty, lh,
                    "RX: " + s.getFrameCount() + "  age="
                            + (age < 0 ? "--" : age + "ms"), Theme.WHITE);
            if (s.getLastError() != null) {
                ty = line(c, t, x + 8f * l.s, ty, lh, "ERR: " + s.getLastError(), Theme.RED);
            }
        }
        if (probe != null) {
            ty = line(c, t, x + 8f * l.s, ty, lh,
                    "PROBE: sensors=" + probe.getSensorCount()
                            + " veh=" + probe.getVehicleCount()
                            + " mapped=" + probe.getMappedCount(), Theme.LABEL);
            ty = line(c, t, x + 8f * l.s, ty, lh,
                    "IVI_CAN_READ: " + (probe.isPermissionGranted() ? "GRANTED" : "DENIED"),
                    probe.isPermissionGranted() ? Theme.LABEL : Theme.RED);
        }

        ty += 4f * l.s;
        ty = channel(c, t, x + 8f * l.s, ty, lh, "RPM", d.rpm, nowMs);
        ty = channel(c, t, x + 8f * l.s, ty, lh, "SPEED", d.speed, nowMs);
        ty = channel(c, t, x + 8f * l.s, ty, lh, "COOLANT", d.coolantTemp, nowMs);
        ty = channel(c, t, x + 8f * l.s, ty, lh, "OIL P", d.oilPressure, nowMs);
        ty = channel(c, t, x + 8f * l.s, ty, lh, "THROTTLE", d.throttle, nowMs);
        ty = channel(c, t, x + 8f * l.s, ty, lh, "BOOST", d.boostActual, nowMs);
        channel(c, t, x + 8f * l.s, ty, lh, "KNOCK", d.knockRetard, nowMs);
    }

    private static float channel(Canvas c, Theme t, float x, float y, float lh,
                                 String name, Channel ch, long nowMs) {
        String src = ch.getSource() == null ? "-" : ch.getSource();
        String age = ch.getUpdatedAtMs() == 0 ? "--" : (nowMs - ch.getUpdatedAtMs()) + "ms";
        int colour = ch.isLive() ? 0xFF6BD07A
                : (ch.isStale() ? Theme.WARN
                : (ch.hasValue() ? Theme.LABEL : Theme.VALUE_DIM));
        return line(c, t, x, y, lh,
                name + " " + ch.text(1) + "  " + ch.getStatusText()
                        + "  " + src + "  " + age
                        + "  n=" + ch.getUpdates() + "  [" + ch.getObservedRange() + "]",
                colour);
    }

    private static float line(Canvas c, Theme t, float x, float y, float lh,
                              String s, int colour) {
        c.drawText(s, x, y, t.text(colour, lh * 0.78f, Paint.Align.LEFT, false));
        return y + lh;
    }
}
