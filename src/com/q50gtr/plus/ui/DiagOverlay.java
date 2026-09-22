package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.EcuTekLiveSource;
import com.q50gtr.plus.data.InTouchVehicleSource;
import com.q50gtr.plus.data.VehicleData;
import com.q50gtr.plus.data.VehicleProbe;

/**
 * Диагностический оверлей: состояние транспорта, источники каналов и список
 * сигналов, которые ГУ отдаёт, а мы ещё не принимаем.
 *
 * По умолчанию выключен и ничего не рисует, поэтому утверждённый визуал он не
 * меняет. Переключается долгим нажатием (около секунды) на часы в верхней
 * полосе: ВЫКЛ -> СОСТОЯНИЕ -> СЕНСОРЫ -> ВЫКЛ.
 *
 * Читается он с фотографии в солнечном салоне, а не в тёмной комнате, поэтому
 * цвета здесь свои, яркие, а не приглушённые из темы приборов: первый снимок с
 * машины наполовину не читался именно из-за серого по чёрному.
 */
public final class DiagOverlay {

    public static final int OFF = 0;
    public static final int STATUS = 1;
    public static final int SENSORS = 2;
    public static final int ECUTEK = 3;
    public static final int BLUETOOTH = 4;
    public static final int PAGES = 5;

    /* Палитра оверлея: максимальный контраст, никакого «приглушённого». */
    private static final int FG = 0xFFFFFFFF;
    private static final int DIM = 0xFFC8D2E0;
    private static final int OK = 0xFF57F08A;
    private static final int HOT = 0xFFFFB13B;
    private static final int ERR = 0xFFFF6B6B;
    private static final int KEY = 0xFF9FC4FF;
    private static final int BG = 0xFF000000;

    private DiagOverlay() {
    }

    /** Версия сборки: без неё непонятно, что именно сейчас на устройстве. */
    private static String version = "?";

    public static void setVersion(String v) {
        version = v == null ? "?" : v;
    }

    public static void draw(Canvas c, Theme t, Layout l, DataHub hub,
                            VehicleProbe probe, DisplayInfo display, long nowMs,
                            int page) {
        if (page == BLUETOOTH) {
            drawBluetooth(c, t, l, hub);
        } else if (page == ECUTEK) {
            drawEcuTek(c, t, l, hub, nowMs);
        } else if (page == SENSORS) {
            drawSensors(c, t, l, hub, probe);
        } else {
            drawStatus(c, t, l, hub, probe, display, nowMs);
        }
    }

    private static void drawStatus(Canvas c, Theme t, Layout l, DataHub hub,
                                   VehicleProbe probe, DisplayInfo display, long nowMs) {
        VehicleData d = hub.getData();

        String[] text = new String[30];
        int[] colour = new int[30];
        int n = 0;

        colour[n] = KEY; text[n++] = "Q50 GTR+ " + version;
        if (display != null) {
            colour[n] = DIM; text[n++] = display.summary();
        }
        colour[n] = DIM;
        text[n++] = "LAYOUT: " + (int) l.w + "x" + (int) l.h
                + "  s=" + l.s + "  r=" + (int) l.dialR;
        colour[n] = KEY; text[n++] = "SOURCE: " + hub.getSourceLabel();
        colour[n] = hub.isLive() ? OK : HOT;
        text[n++] = hub.isLive() ? "STATE:  LIVE" : "STATE:  DEMO (нет связи)";

        if (hub.getInTouch() instanceof InTouchVehicleSource) {
            InTouchVehicleSource s = (InTouchVehicleSource) hub.getInTouch();
            colour[n] = FG;
            text[n++] = "INTOUCH: " + (s.isConnected() ? "CONNECTED" : "DISCONNECTED")
                    + "  bound=" + s.getBoundCount();
            long age = s.getLastFrameMs() == 0 ? -1 : nowMs - s.getLastFrameMs();
            colour[n] = FG;
            text[n++] = "RX: " + s.getFrameCount() + "  age="
                    + (age < 0 ? "--" : age + "ms");
            if (s.getLastError() != null) {
                colour[n] = ERR; text[n++] = "ERR: " + s.getLastError();
            }
        }
        if (probe != null) {
            colour[n] = FG;
            text[n++] = "PROBE: sensors=" + probe.getSensorCount()
                    + " veh=" + probe.getVehicleCount()
                    + " mapped=" + probe.getMappedCount()
                    + " free=" + probe.getUnmappedNames().size();
            colour[n] = probe.isPermissionGranted() ? FG : ERR;
            text[n++] = "IVI_CAN_READ: "
                    + (probe.isPermissionGranted() ? "GRANTED" : "DENIED");
        }

        if (hub.getBridge() instanceof com.q50gtr.plus.net.BridgeSource) {
            com.q50gtr.plus.net.BridgeSource b =
                    (com.q50gtr.plus.net.BridgeSource) hub.getBridge();
            colour[n] = b.getPacketCount() > 0 ? OK : DIM;
            text[n++] = "МОСТ: UDP " + com.q50gtr.plus.net.BridgeSource.PORT
                    + "  пакетов=" + b.getPacketCount()
                    + (b.getLastSender() == null ? "" : "  от " + b.getLastSender());
            colour[n] = DIM;
            text[n++] = "АДРЕС ГУ: " + b.getLocalAddresses();
            if (b.getLastError() != null) {
                colour[n] = ERR; text[n++] = "МОСТ ERR: " + b.getLastError();
            }
        }

        colour[n] = 0; text[n++] = null;   // пустая строка

        // Обороты восстанавливаются из мощности и момента, когда прямой
        // сенсор молчит. Обе исходные величины показаны рядом, чтобы гипотезу
        // можно было проверить прямо с фотографии, а не на слово.
        String calc = "—";
        if (d.enginePower.hasValue() && d.engineTorque.hasValue()
                && d.engineTorque.getValue() >= 1f) {
            calc = Channel.format(d.enginePower.getValue() / d.engineTorque.getValue(), 0);
        }
        colour[n] = KEY;
        text[n++] = "POWER/TORQUE = " + d.enginePower.text(0) + " / "
                + d.engineTorque.text(0) + " = " + calc + " rpm";
        colour[n] = FG;
        text[n++] = "GEAR: " + d.gearText() + "  (сырое " + d.gearPosition.text(0) + ")";
        colour[n] = 0; text[n++] = null;

        n = channel(text, colour, n, "RPM", d.rpm, nowMs);
        n = channel(text, colour, n, "SPEED", d.speed, nowMs);
        n = channel(text, colour, n, "COOLANT", d.coolantTemp, nowMs);
        n = channel(text, colour, n, "OIL P", d.oilPressure, nowMs);
        n = channel(text, colour, n, "THROTTLE", d.throttle, nowMs);
        n = channel(text, colour, n, "BOOST", d.boostActual, nowMs);
        n = channel(text, colour, n, "KNOCK", d.knockRetard, nowMs);

        panel(c, t, l, text, colour, n, 1);
    }

    /**
     * Сырые значения всех автомобильных сенсоров. Именно эта страница
     * отвечает на вопрос «наша привязка промахнулась или сигнала нет»:
     * канал в обоих случаях пустой, а сырой срез различает их сразу.
     */
    private static void drawSensors(Canvas c, Theme t, Layout l, DataHub hub,
                                    VehicleProbe probe) {
        String[] raw = null;
        if (hub.getInTouch() instanceof InTouchVehicleSource) {
            raw = ((InTouchVehicleSource) hub.getInTouch()).getRawLines();
        }
        if (raw == null || raw.length == 0) {
            // Источник не поднялся — показываем хотя бы то, что нашёл зонд.
            java.util.List<String> free = probe == null
                    ? new java.util.ArrayList<String>() : probe.getUnmappedNames();
            raw = new String[free.size()];
            for (int i = 0; i < raw.length; i++) {
                String v = free.get(i);
                raw[i] = v != null && v.startsWith("VS_ID_") ? v.substring(6) : v;
            }
        }

        String[] text = new String[raw.length + 2];
        int[] colour = new int[raw.length + 2];
        int n = 0;
        colour[n] = KEY;
        text[n++] = "СЫРЫЕ СЕНСОРЫ: " + raw.length + "  (префикс VS_ID_ убран)";
        colour[n] = 0; text[n++] = null;
        for (int i = 0; i < raw.length; i++) {
            colour[n] = FG;
            text[n++] = raw[i];
        }
        panel(c, t, l, text, colour, n, 2);
    }

    /**
     * Рисует панель, сама подбирая размер шрифта и число колонок так, чтобы
     * все строки поместились в экран. Раньше высота была задана константой, и
     * строки, не влезшие в неё, просто пропадали за краем.
     */
    private static void panel(Canvas c, Theme t, Layout l, String[] text, int[] colour,
                              int n, int columns) {
        float pad = 8f * l.s;
        float top = 50f * l.s;
        float bottom = l.h - 8f * l.s;
        float maxH = bottom - top;

        int rows = (n + columns - 1) / columns;
        float lh = (maxH - 2f * pad) / rows;
        float maxLh = 19f * l.s;
        if (lh > maxLh) {
            lh = maxLh;
        }
        float size = lh * 0.80f;

        float colW = 0f;
        Paint m = t.text(FG, size, Paint.Align.LEFT, false);
        for (int i = 0; i < n; i++) {
            if (text[i] == null) {
                continue;
            }
            float wv = m.measureText(text[i]);
            if (wv > colW) {
                colW = wv;
            }
        }
        colW += 12f * l.s;

        float w = colW * columns + 2f * pad;
        float maxW = l.w - 16f * l.s;
        if (w > maxW) {
            w = maxW;
            colW = (w - 2f * pad) / columns;
        }
        float h = rows * lh + 2f * pad;
        float x = l.w - w - 8f * l.s;
        float y = top;

        // Фон почти непрозрачный: на солнце полупрозрачная подложка давала
        // серый текст на сером приборе.
        t.rect.set(x, y, x + w, y + h);
        c.drawRoundRect(t.rect, 4f, 4f, t.fill(BG));
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(KEY, 1.5f));

        for (int i = 0; i < n; i++) {
            if (text[i] == null) {
                continue;
            }
            int col = i / rows;
            int row = i % rows;
            c.drawText(text[i], x + pad + col * colW,
                    y + pad + (row + 1) * lh - lh * 0.22f,
                    t.text(colour[i], size, Paint.Align.LEFT, false));
        }
    }

    /**
     * Состояние связи с адаптером EcuTek EVI. Отдельной страницей, потому что
     * это другой транспорт с другими отказами: Bluetooth, а не шина ГУ.
     */
    private static void drawEcuTek(Canvas c, Theme t, Layout l, DataHub hub, long nowMs) {
        String[] text = new String[32];
        int[] colour = new int[32];
        int n = 0;

        if (!(hub.getEcuTek() instanceof EcuTekLiveSource)) {
            colour[n] = ERR; text[n++] = "EcuTek-источник не собран";
            panel(c, t, l, text, colour, n, 1);
            return;
        }
        EcuTekLiveSource e = (EcuTekLiveSource) hub.getEcuTek();
        int st = e.getState();
        int col = st == EcuTekLiveSource.STREAMING ? OK
                : (st == EcuTekLiveSource.CONNECTED
                        || st == EcuTekLiveSource.SESSION_STARTING ? KEY
                        : (st == EcuTekLiveSource.ERROR ? ERR : HOT));
        colour[n] = col; text[n++] = "ECUTEK: " + e.getStateName();
        colour[n] = FG;
        text[n++] = "EVI: " + (e.getDeviceName() == null ? "(не найден)" : e.getDeviceName());
        colour[n] = DIM;
        text[n++] = "MAC: " + (e.getDeviceAddress() == null ? "-" : e.getDeviceAddress());

        long age = e.getLastRxMs() == 0 ? -1 : nowMs - e.getLastRxMs();
        colour[n] = FG;
        text[n++] = "RX: " + e.getPacketCount() + " пакетов, " + e.getBytesIn() + " байт";
        colour[n] = FG;
        text[n++] = "LAST PACKET: " + (age < 0 ? "--" : age + " ms");
        colour[n] = e.getRawLog().isEnabled() ? OK : DIM;
        text[n++] = "RAW LOG: " + (e.getRawLog().isEnabled()
                ? "ВКЛ, строк " + e.getRawLog().getLineCount()
                : "выкл (маркер " + com.q50gtr.plus.ecutek.EcuTekRawLog.MARKER + " на флешке)");
        if (e.getLastError() != null) {
            colour[n] = ERR; text[n++] = "ERROR: " + e.getLastError();
        }

        colour[n] = 0; text[n++] = null;
        colour[n] = DIM;
        text[n++] = "Разбора кадров нет: протокол не подтверждён.";
        colour[n] = DIM;
        text[n++] = "Каналы ниже ждут его и показывают прочерк.";
        colour[n] = 0; text[n++] = null;

        VehicleData d = hub.getData();
        n = channel(text, colour, n, "RPM", d.rpm, nowMs);
        n = channel(text, colour, n, "BOOST", d.boostActual, nowMs);
        n = channel(text, colour, n, "AFR B1", d.afrB1, nowMs);
        n = channel(text, colour, n, "IGNITION", d.ignitionTiming, nowMs);
        n = channel(text, colour, n, "HPFP", d.hpfpActual, nowMs);
        for (int i = 0; i < d.knockIndex.length && n < text.length - 1; i++) {
            n = channel(text, colour, n, "KI" + (i + 1), d.knockIndex[i], nowMs);
        }
        panel(c, t, l, text, colour, n, 1);
    }

    /**
     * Полный отчёт зонда Bluetooth прямо на экране.
     *
     * Он и раньше собирался, но уходил только в архив на флешке — а спор о
     * том, есть ли на ГУ Bluetooth, решается именно этими строками. Со
     * списком сенсоров сработало ровно это: показать на экране то, что иначе
     * пришлось бы везти файлом.
     */
    /**
     * Кнопка «Проверить Bluetooth» на странице BLUETOOTH. Прямоугольник
     * считается в одном месте и для отрисовки, и для попадания пальцем —
     * иначе они разъезжаются.
     */
    public static void buttonRect(Layout l, float[] out) {
        float w = 300f * l.s;
        float h = 34f * l.s;
        out[0] = l.w - w - 12f * l.s;
        out[1] = l.h - h - 10f * l.s;
        out[2] = out[0] + w;
        out[3] = out[1] + h;
    }

    public static boolean hitButton(Layout l, float x, float y) {
        float[] r = new float[4];
        buttonRect(l, r);
        return x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3];
    }

    private static void drawButton(Canvas c, Theme t, Layout l) {
        float[] r = new float[4];
        buttonRect(l, r);
        t.rect.set(r[0], r[1], r[2], r[3]);
        c.drawRoundRect(t.rect, 4f, 4f, t.fill(0xFF1D2B4A));
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(KEY, 1.5f));
        c.drawText("ПРОВЕРИТЬ BLUETOOTH  +  ЭКСПОРТ",
                (r[0] + r[2]) * 0.5f, r[3] - 11f * l.s,
                t.text(FG, 14f * l.s, Paint.Align.CENTER, false));
    }

    private static void drawBluetooth(Canvas c, Theme t, Layout l, DataHub hub) {
        String report = null;
        if (hub.getEcuTek() instanceof EcuTekLiveSource) {
            EcuTekLiveSource e = (EcuTekLiveSource) hub.getEcuTek();
            if (e.getProbe() != null) {
                report = e.getProbe().getReport();
            }
        }
        if (report == null || report.length() == 0) {
            String[] one = {"Зонд Bluetooth ещё не отработал"};
            int[] col = {HOT};
            panel(c, t, l, one, col, 1, 1);
            drawButton(c, t, l);
            return;
        }
        String[] raw = report.split("\n");
        String[] text = new String[raw.length + 1];
        int[] colour = new int[raw.length + 1];
        int n = 0;
        colour[n] = KEY; text[n++] = "ЗОНД BLUETOOTH";
        for (int i = 0; i < raw.length && n < text.length; i++) {
            String ln = raw[i];
            if (ln == null || ln.trim().length() == 0) {
                continue;
            }
            String low = ln.toLowerCase();
            if (low.indexOf("совпал") >= 0) {
                colour[n] = low.indexOf("не совпал") >= 0 ? HOT : OK;
            } else if (low.indexOf("нет") >= 0 || low.indexOf("null") >= 0
                    || low.indexOf("= false") >= 0) {
                colour[n] = HOT;
            } else if (low.indexOf("важно") >= 0) {
                colour[n] = OK;
            } else {
                colour[n] = ln.startsWith("--") ? KEY : FG;
            }
            text[n++] = ln;
        }
        panel(c, t, l, text, colour, n, n > 20 ? 2 : 1);
        drawButton(c, t, l);
    }

    private static int channel(String[] text, int[] colour, int n,
                               String name, Channel ch, long nowMs) {
        String src = ch.getSource() == null ? "-" : ch.getSource();
        String age = ch.getUpdatedAtMs() == 0 ? "--" : (nowMs - ch.getUpdatedAtMs()) + "ms";
        colour[n] = ch.isLive() ? OK : (ch.isStale() ? HOT : (ch.hasValue() ? DIM : ERR));
        text[n] = name + " " + ch.text(1) + "  " + ch.getStatusText()
                + "  " + src + "  " + age
                + "  n=" + ch.getUpdates() + "  [" + ch.getObservedRange() + "]";
        return n + 1;
    }
}
