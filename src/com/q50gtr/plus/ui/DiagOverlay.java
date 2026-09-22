package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.InTouchVehicleSource;
import com.q50gtr.plus.data.VehicleData;
import com.q50gtr.plus.data.VehicleProbe;

/**
 * Диагностический оверлей: мост через телефон и состояние каналов.
 *
 * По умолчанию выключен и ничего не рисует, поэтому утверждённый визуал он не
 * меняет. Переключается долгим нажатием (около секунды) на часы в верхней
 * полосе: ВЫКЛ -> МОСТ -> СОСТОЯНИЕ -> ВЫКЛ. Первым идёт мост: именно его
 * открывают у машины, и лишние нажатия по дороге к нему — потерянное время.
 *
 * Читается он с фотографии в солнечном салоне, а не в тёмной комнате, поэтому
 * цвета здесь свои, яркие, а не приглушённые из темы приборов: первый снимок с
 * машины наполовину не читался именно из-за серого по чёрному.
 */
public final class DiagOverlay {

    /*
     * Страниц осталось две, и первая — та, ради которой оверлей сейчас
     * открывают. Раньше их было пять: СЕНСОРЫ, ECUTEK и BLUETOOTH отвечали
     * на вопросы, которые давно закрыты — привязка сенсоров снята и
     * записана, прямой путь до EVI признан невозможным, Bluetooth на этом
     * слое Android доказанно отсутствует. Листать их по дороге к мосту
     * значило тратить нажатия впустую. Отчёты всех трёх по-прежнему пишутся
     * в архив на флешке: с экрана ушли страницы, а не доказательства.
     */
    public static final int OFF = 0;
    public static final int BRIDGE = 1;
    public static final int STATUS = 2;
    public static final int PAGES = 3;

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
    private static com.q50gtr.plus.diag.TransportProbe transport;

    public static void setTransport(com.q50gtr.plus.diag.TransportProbe p) {
        transport = p;
    }

    /** Приёмник моста: подпись на кнопке зависит от того, что он уже пробовал. */
    private static com.q50gtr.plus.net.BridgeSource bridge;

    public static void setBridge(com.q50gtr.plus.net.BridgeSource b) {
        bridge = b;
    }

    public static void setVersion(String v) {
        version = v == null ? "?" : v;
    }

    public static void draw(Canvas c, Theme t, Layout l, DataHub hub,
                            VehicleProbe probe, DisplayInfo display, long nowMs,
                            int page) {
        if (page == BRIDGE) {
            drawBridge(c, t, l, hub);
            drawButton(c, t, l, buttonLabel(BRIDGE));
        } else {
            drawStatus(c, t, l, hub, probe, display, nowMs);
        }
    }

    /**
     * Мост: всё про сеть на одном экране, без листания.
     *
     * Порядок строк — порядок вопросов, которые задаёшь у машины: дошёл ли
     * пакет, какой у ГУ адрес, жив ли провод, есть ли маршрут, отвечает ли
     * телефон. Каждая следующая отвечает, почему предыдущая пуста, поэтому
     * снимок этой страницы — законченный ответ, а не половина.
     */
    private static void drawBridge(Canvas c, Theme t, Layout l, DataHub hub) {
        String[] text = new String[24];
        int[] colour = new int[24];
        int n = 0;

        colour[n] = KEY; text[n++] = "МОСТ ЧЕРЕЗ ТЕЛЕФОН   " + version;
        if (!(hub.getBridge() instanceof com.q50gtr.plus.net.BridgeSource)) {
            colour[n] = ERR; text[n++] = "приёмник не создан";
            panel(c, t, l, text, colour, n, 1);
            return;
        }
        com.q50gtr.plus.net.BridgeSource b =
                (com.q50gtr.plus.net.BridgeSource) hub.getBridge();

        colour[n] = b.getPacketCount() > 0 ? OK : HOT;
        text[n++] = "ПРИЁМ: UDP " + com.q50gtr.plus.net.BridgeSource.PORT
                + "  пакетов=" + b.getPacketCount()
                + (b.getLastSender() == null ? "" : "  от " + b.getLastSender());
        // Единственная строка, которую надо переписать в телефон. Адрес
        // теперь выдаёт сам телефон по DHCP и он каждый раз другой, так что
        // назвать его заранее в инструкции нельзя — только показать здесь.
        String me = com.q50gtr.plus.diag.TransportProbe.ifaceAddress("usb0");
        colour[n] = me == null ? ERR : OK;
        text[n++] = "СЛАТЬ С ТЕЛЕФОНА НА: "
                + (me == null ? "адреса нет" : me + " : "
                        + com.q50gtr.plus.net.BridgeSource.PORT);
        colour[n] = FG;
        text[n++] = "АДРЕС ГУ: " + b.getLocalAddresses()
                + "   телефон: "
                + (com.q50gtr.plus.diag.TransportProbe.gatewayFor("usb0") == null
                        ? "?" : com.q50gtr.plus.diag.TransportProbe.gatewayFor("usb0"));
        colour[n] = FG;
        text[n++] = "usb0: " + b.getIfaceCounters("usb0");
        colour[n] = FG;
        text[n++] = "ЛИНК: "
                + com.q50gtr.plus.diag.TransportProbe.linkDetails("usb0");
        colour[n] = DIM;
        text[n++] = "МАРШРУТЫ: " + com.q50gtr.plus.diag.TransportProbe.routes();
        // Телефон отвечает на ARP или нет — единственная строка, которая
        // отделяет «нас не слышат» от «нам нечем отправить».
        colour[n] = com.q50gtr.plus.diag.TransportProbe.isPhoneAnswering() ? OK : ERR;
        text[n++] = "ARP: " + com.q50gtr.plus.diag.TransportProbe.arp();
        colour[n] = DIM;
        text[n++] = "ПЕРЕДАЧА: " + b.getTxResult();
        colour[n] = DIM;
        text[n++] = "DHCP: " + (transport == null ? "-" : transport.getDhcpResult());
        colour[n] = b.isBeaconOn() ? OK : DIM;
        text[n++] = "МАЯК: " + b.getBeaconState();
        colour[n] = b.getPacketCount() > 0 ? OK : DIM;
        text[n++] = "ТЕСТ: " + b.getTestInfo();
        colour[n] = DIM;
        text[n++] = "USB: " + b.getUsbDevices();
        if (transport != null && transport.getLoadResult() != null) {
            colour[n] = FG; text[n++] = "insmod: " + transport.getLoadResult();
        }
        if (transport != null && transport.getReport().length() > 0) {
            colour[n] = DIM; text[n++] = "ТРАНСПОРТ: " + transport.summary();
            colour[n] = DIM; text[n++] = "БЕЗ ПРОВОДА: " + transport.getWireless();
        }
        if (b.getLastError() != null) {
            colour[n] = ERR; text[n++] = "ОШИБКА: " + b.getLastError();
        }
        panel(c, t, l, text, colour, n, 1);
    }

    private static void drawStatus(Canvas c, Theme t, Layout l, DataHub hub,
                                   VehicleProbe probe, DisplayInfo display, long nowMs) {
        VehicleData d = hub.getData();

        // Запас намеренный: строки диагностики добавляются по ходу работы,
        // а выход за массив обрушит отрисовку приборов целиком.
        String[] text = new String[40];
        int[] colour = new int[40];
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
     * Рисует панель ЛИСТАЯ, а не ужимая.
     *
     * Раньше шрифт сжимался до тех пор, пока весь отчёт не влезал в экран —
     * и на 800x480 сотня строк превращалась в нечитаемую сетку, которую
     * невозможно снять на телефон. Теперь размер строки фиксирован и
     * комфортен, а что не поместилось, уходит на следующую страницу.
     * Листание — касанием правой или левой половины панели.
     */
    private static int totalPages = 1;
    private static int textPage;

    public static void nextTextPage(int dir) {
        textPage += dir;
        if (textPage < 0) {
            textPage = totalPages - 1;
        }
        if (textPage >= totalPages) {
            textPage = 0;
        }
    }

    public static void resetTextPage() {
        textPage = 0;
    }

    /** Есть ли что листать на текущей странице. */
    public static boolean hasMoreThanOnePage() {
        return totalPages > 1;
    }

    private static void panel(Canvas c, Theme t, Layout l, String[] text, int[] colour,
                              int n, int columns) {
        float pad = 8f * l.s;
        float top = 50f * l.s;
        float bottom = l.h - 8f * l.s;
        float maxH = bottom - top;

        // Ниже этого читать с фотографии уже нельзя, проверено на снимках из
        // машины. Поэтому размер не уменьшается — растёт число страниц.
        float lh = 26f * l.s;
        float size = lh * 0.78f;
        int rows = (int) ((maxH - 2f * pad - lh) / lh);
        if (rows < 1) {
            rows = 1;
        }
        int perPage = rows * columns;
        totalPages = (n + perPage - 1) / perPage;
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (textPage >= totalPages) {
            textPage = 0;
        }
        int from = textPage * perPage;
        int to = from + perPage;
        if (to > n) {
            to = n;
        }

        // Ширина колонки считается по самой длинной строке страницы. Если
        // в две колонки строки не помещаются, колонка остаётся ОДНА: лучше
        // больше страниц, чем текст, наползающий сам на себя. Именно это и
        // случилось на первом снимке после перехода к фиксированному шрифту.
        float maxW = l.w - 16f * l.s;
        float longest = 0f;
        Paint m = t.text(FG, size, Paint.Align.LEFT, false);
        for (int i = 0; i < n; i++) {
            if (text[i] == null) {
                continue;
            }
            float wv = m.measureText(text[i]);
            if (wv > longest) {
                longest = wv;
            }
        }
        if (columns > 1 && longest + 24f * l.s > (maxW - 2f * pad) / columns) {
            columns = 1;
            rows = (int) ((maxH - 2f * pad - lh) / lh);
            perPage = rows;
            totalPages = (n + perPage - 1) / perPage;
            if (totalPages < 1) {
                totalPages = 1;
            }
            if (textPage >= totalPages) {
                textPage = 0;
            }
            from = textPage * perPage;
            to = from + perPage;
            if (to > n) {
                to = n;
            }
        }

        float colW = 0f;
        for (int i = from; i < to; i++) {
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
        if (w > maxW) {
            w = maxW;
            colW = (w - 2f * pad) / columns;
        }
        float h = (to - from + columns - 1) / columns * lh + 2f * pad + lh;
        if (h > maxH) {
            h = maxH;
        }
        float x = l.w - w - 8f * l.s;
        float y = top;

        // Фон почти непрозрачный: на солнце полупрозрачная подложка давала
        // серый текст на сером приборе.
        t.rect.set(x, y, x + w, y + h);
        c.drawRoundRect(t.rect, 4f, 4f, t.fill(BG));
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(KEY, 1.5f));

        for (int i = from; i < to; i++) {
            if (text[i] == null) {
                continue;
            }
            int idx = i - from;
            int col = idx / rows;
            int row = idx % rows;
            Paint tp = t.text(colour[i], size, Paint.Align.LEFT, false);
            c.drawText(fit(text[i], tp, colW - 10f * l.s), x + pad + col * colW,
                    y + pad + (row + 1) * lh - lh * 0.22f, tp);
        }

        // Подвал: какая страница и как листать. Без него не догадаться, что
        // отчёт длиннее экрана.
        if (totalPages > 1) {
            c.drawText("стр. " + (textPage + 1) + " / " + totalPages
                            + "   — касание слева/справа листает —",
                    x + w * 0.5f, y + h - lh * 0.30f,
                    t.text(KEY, size * 0.95f, Paint.Align.CENTER, false));
        }
    }

    /** Укорачивает строку до ширины колонки, чтобы она не лезла в соседнюю. */
    private static String fit(String s, Paint p, float w) {
        if (p.measureText(s) <= w) {
            return s;
        }
        int lo = 1;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (p.measureText(s.substring(0, mid) + "…") <= w) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return s.substring(0, lo) + "…";
    }

    /** Попадание в панель: нужно, чтобы отличить листание от смены вкладки. */
    public static boolean hitPanel(Layout l, float x, float y) {
        return y > 50f * l.s && y < l.h - 8f * l.s;
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

    /** Есть ли на этой странице кнопка, и какая. null — кнопки нет. */
    public static String buttonLabel(int page) {
        if (page != BRIDGE) {
            return null;
        }
        if (transport == null) {
            return "РАЗВЕДКА ЕЩЁ ИДЁТ";
        }
        // Одна кнопка, один проход: модуль, интерфейс, адрес, проба, маяк.
        // Пошаговый вариант был нужен, пока каждый шаг был под вопросом.
        if (bridge != null && bridge.isBeaconOn()) {
            return "ВЫКЛЮЧИТЬ МАЯК";
        }
        if (!com.q50gtr.plus.diag.TransportProbe.hasIface("usb0")
                && !transport.isRndisLoaded()) {
            return "ПОДНЯТЬ МОСТ  (нужен root)";
        }
        if (!com.q50gtr.plus.diag.TransportProbe.hasIface("usb0")) {
            return "ПОДКЛЮЧИТЕ ТЕЛЕФОН, ВКЛЮЧИТЕ USB-МОДЕМ";
        }
        String a = com.q50gtr.plus.diag.TransportProbe.ifaceAddress("usb0");
        return a == null ? "ПОДНЯТЬ МОСТ" : "ПОДНЯТЬ МОСТ  (usb0 = " + a + ")";
    }

    private static void drawButton(Canvas c, Theme t, Layout l, String label) {
        float[] r = new float[4];
        buttonRect(l, r);
        t.rect.set(r[0], r[1], r[2], r[3]);
        c.drawRoundRect(t.rect, 4f, 4f, t.fill(0xFF1D2B4A));
        c.drawRoundRect(t.rect, 4f, 4f, t.stroke(KEY, 1.5f));
        c.drawText(label, (r[0] + r[2]) * 0.5f, r[3] - 11f * l.s,
                t.text(FG, 14f * l.s, Paint.Align.CENTER, false));
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
