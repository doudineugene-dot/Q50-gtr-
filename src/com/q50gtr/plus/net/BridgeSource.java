package com.q50gtr.plus.net;

import android.util.Log;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.DataSource;
import com.q50gtr.plus.data.VehicleData;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Приём телеметрии по сети — мост через телефон.
 *
 * Зачем. Прямой путь до EcuTek EVI-6087 с этого ГУ закрыт: адаптер в
 * исполнении BLE, а публичного BLE API в Android до API 18 нет, здесь API 10
 * (docs/ECUTEK-EVI-6087.md). Телефон же и BLE умеет, и с OBD-адаптером по
 * классическому Bluetooth разговаривает. Значит читать может он, а ГУ —
 * показывать.
 *
 * Эта половина моста сознательно не знает ни про EcuTek, ни про ELM327.
 * Протокол EcuTek до сих пор не разобран, и привязывать к нему приёмник
 * значило бы снова упереться в неизвестное. Здесь формат свой, простой и
 * описанный до последнего байта:
 *
 * <pre>
 *   UDP, порт 45455, текст UTF-8, по строке на параметр:
 *       RPM=1234.5
 *       BOOST=0.42
 *       KI1=0.3
 * </pre>
 *
 * Неизвестные ключи молча пропускаются — отправитель может слать больше,
 * чем мы умеем показать. Значения вне физически возможного диапазона
 * отбрасываются: на приборке автомобиля лучше прочерк, чем мусор из сети.
 *
 * ТЕЛЕМЕТРИЯ ИДЁТ ТОЛЬКО В ОДНУ СТОРОНУ. Сокет открыт на приём; в сторону
 * автомобиля отсюда не уходит ничего и никогда. Единственное исключение —
 * txTest(): по явному нажатию кнопки диагностики ГУ шлёт телефону строку
 * «Q50GTR HELLO», чтобы проверить, способно ли оно вообще передавать. Это
 * проба связи, а не команда: ни автомобиль, ни адаптер её не видят.
 */
public final class BridgeSource implements DataSource {

    public static final String NAME = "BRIDGE";
    public static final int PORT = 45455;

    private static final String TAG = "Q50GTR/BRIDGE";
    private static final long STALE_AFTER_MS = 2000L;

    private DatagramSocket socket;
    private Thread worker;
    private volatile boolean running;

    private volatile int packets;
    private volatile long lastRxMs;
    private volatile String lastError;
    private volatile String lastSender;

    /*
     * Тестовый канал. Прежде чем гнать по мосту телеметрию, надо доказать
     * сам мост: что пакеты доходят, не теряются и не приходят задом наперёд.
     * Для этого отправитель шлёт TEST_COUNTER (растущее целое) и
     * TEST_MS (свои миллисекунды). По ним видно и пропуски, и задержку.
     */
    private volatile long testCounter = -1;
    private volatile long testGaps;
    private volatile long testLatencyMs = Long.MIN_VALUE;

    /** Принятые значения ждут тика UI: в поток приёма лезть отрисовке нельзя. */
    private final Object lock = new Object();
    private final java.util.HashMap<String, float[]> pending =
            new java.util.HashMap<String, float[]>();

    public String getName() {
        return NAME;
    }

    public int getPacketCount() {
        return packets;
    }

    public long getLastRxMs() {
        return lastRxMs;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastSender() {
        return lastSender;
    }

    /** Строка для диагностического экрана: жив ли канал и как он себя ведёт. */
    public String getTestInfo() {
        if (testCounter < 0) {
            return "тестовых пакетов не было";
        }
        return "счётчик=" + testCounter + "  пропусков=" + testGaps
                + (testLatencyMs == Long.MIN_VALUE ? "" : "  сдвиг=" + testLatencyMs + "ms");
    }

    /**
     * Перечень сетевых устройств ядра прямо сейчас. Обновляется на каждом
     * кадре: если воткнуть в ГУ телефон в режиме USB-модема, здесь должен
     * появиться новый интерфейс — и это видно сразу, без перезапуска.
     */
    public String getKernelInterfaces() {
        try {
            java.io.File[] n = new java.io.File("/sys/class/net").listFiles();
            if (n == null || n.length == 0) {
                return "пусто";
            }
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < n.length; i++) {
                if (b.length() > 0) {
                    b.append(' ');
                }
                b.append(n[i].getName());
            }
            return b.toString();
        } catch (Throwable t) {
            return "не прочитать";
        }
    }

    /**
     * Что сейчас висит на шине USB. Обновляется на каждом кадре.
     *
     * Нужно, чтобы отличить три разные причины, которые выглядят одинаково
     * («не работает»): порт только для зарядки, кабель только для зарядки,
     * либо устройство перечислилось, но режим передачи данных не выбран. В
     * первых двух случаях здесь ничего нового не появится, в третьем —
     * появится имя телефона.
     */
    public String getUsbDevices() {
        try {
            java.io.File[] d = new java.io.File("/sys/bus/usb/devices").listFiles();
            if (d == null || d.length == 0) {
                return "шина не читается";
            }
            StringBuilder b = new StringBuilder();
            int n = 0;
            for (int i = 0; i < d.length && n < 6; i++) {
                String prod = read1(d[i].getPath() + "/product");
                if (prod == null) {
                    continue;   // корневые хабы без имени не интересны
                }
                String man = read1(d[i].getPath() + "/manufacturer");
                if (b.length() > 0) {
                    b.append(" | ");
                }
                b.append(man == null ? "" : man + " ").append(prod);
                n++;
            }
            return b.length() == 0 ? "устройств с именем нет" : b.toString();
        } catch (Throwable t) {
            return "не прочитать";
        }
    }

    private static String read1(String path) {
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(new java.io.FileReader(path), 256);
            String s = r.readLine();
            return s == null ? null : s.trim();
        } catch (Throwable t) {
            return null;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Счётчики интерфейса из /proc/net/dev. Это главный разделитель причин.
     *
     * Если RX растёт, а пакетов у нас ноль — кадры доходят до интерфейса, и
     * теряются выше: сокет, порт, фильтр. Если RX стоит на нуле — до ГУ не
     * долетает ничего, и разбираться надо на стороне телефона: маршрут,
     * адрес, включённая раздача. Без этой строки обе причины выглядят
     * одинаково, и их можно перебирать бесконечно.
     */
    public String getIfaceCounters(String name) {
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(new java.io.FileReader("/proc/net/dev"), 4096);
            String ln;
            while ((ln = r.readLine()) != null) {
                int c = ln.indexOf(':');
                if (c < 0 || !ln.substring(0, c).trim().equals(name)) {
                    continue;
                }
                String[] f = ln.substring(c + 1).trim().split("\\s+");
                if (f.length < 10) {
                    return "строка не разобрана";
                }
                // 0 байт, 1 пакетов на приём; 8 байт, 9 пакетов на передачу.
                // Состояние впереди: без него счётчики обманывают. На машине
                // было RX 4 при TX 0 — кадры шли, а ядро не отвечало даже на
                // ARP, потому что интерфейс был опущен.
                long rx = parseCounter(f[1]);
                sampleRx(rx);
                return (com.q50gtr.plus.diag.TransportProbe.isIfaceUp(name) ? "UP" : "DOWN")
                        + "  RX " + f[1] + " пак (" + rxGrowth() + ") / " + f[0]
                        + " байт   TX " + f[9] + " пак";
            }
            return "интерфейса нет";
        } catch (Throwable t) {
            return "не прочитать";
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Проба передачи. На машине usb0 поднялся и получил адрес, а счётчик TX
     * так и остался на нуле: ГУ не отправило ни кадра, хотя приняло 47.
     * Снаружи у этого две причины, и выглядят они одинаково — либо ядру
     * некуда слать (нет маршрута), либо драйвер не отдаёт кадры в провод
     * (нет несущей). Разделяет их только попытка отправить.
     *
     * Поэтому здесь ГУ шлёт само: на адрес телефона, на широковещательный
     * адрес подсети и на сам себя. Текст ошибки сокета и прирост TX в ядре
     * дают ответ прямо на экране.
     *
     * Отправляется безобидная метка на тот же порт моста: это не команда
     * ни автомобилю, ни адаптеру.
     */
    public String txTest() {
        StringBuilder b = new StringBuilder();
        long before = txPackets("usb0");
        String[] targets = probeTargets();
        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
            s.setBroadcast(true);
            byte[] msg = "Q50GTR HELLO\n".getBytes("UTF-8");
            for (int i = 0; i < targets.length; i++) {
                if (b.length() > 0) {
                    b.append("  ");
                }
                // Метка роли, а не октет: адреса теперь приходят от телефона
                // и могут быть любыми, а «телефон» и «шир» читаются всегда.
                int sp = targets[i].indexOf(' ');
                String label = targets[i].substring(0, sp);
                String ip = targets[i].substring(sp + 1);
                b.append(label).append('=');
                try {
                    s.send(new DatagramPacket(msg, msg.length,
                            InetAddress.getByName(ip), PORT));
                    b.append("ушёл");
                } catch (Throwable t) {
                    // Текст важен целиком: ENETUNREACH и EACCES означают
                    // совершенно разные поломки, а «не отправилось» — ничего.
                    b.append(reason(t));
                }
            }
        } catch (Throwable t) {
            b.append("сокет не создан: ").append(reason(t));
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Throwable ignored) {
                }
            }
        }
        long after = txPackets("usb0");
        b.append("   TX ").append(before < 0 ? "?" : Long.toString(before))
                .append("→").append(after < 0 ? "?" : Long.toString(after));
        String r = b.toString();
        Log.i(TAG, "проба передачи: " + r);
        txResult = r;
        return r;
    }

    /**
     * Куда стучаться пробой. Адреса не зашиты: на машине телефон раздал
     * 10.174.142.0/24, а не 192.168.42.0/24, который я предполагал, и проба
     * всё это время била мимо. Теперь и шлюз, и широковещательный адрес
     * берутся из того, что ядру выдал сам телефон.
     */
    private String[] probeTargets() {
        java.util.ArrayList<String> t = new java.util.ArrayList<String>(3);
        String gw = com.q50gtr.plus.diag.TransportProbe.gatewayFor("usb0");
        if (gw != null) {
            t.add("телефон " + gw);
        }
        String self = null;
        try {
            NetworkInterface ni = NetworkInterface.getByName("usb0");
            if (ni != null) {
                java.util.List<java.net.InterfaceAddress> as = ni.getInterfaceAddresses();
                for (int i = 0; as != null && i < as.size(); i++) {
                    java.net.InterfaceAddress ia = as.get(i);
                    if (ia.getAddress() == null
                            || ia.getAddress().getHostAddress().indexOf(':') >= 0) {
                        continue;
                    }
                    self = ia.getAddress().getHostAddress();
                    if (ia.getBroadcast() != null) {
                        t.add("шир " + ia.getBroadcast().getHostAddress());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (self != null) {
            t.add("сам " + self);
        }
        if (t.isEmpty()) {
            // Совсем без адресов проба бессмысленна, но молчать хуже:
            // пусть отчёт покажет отказ на заведомом адресе, чем пустоту.
            t.add("шир 255.255.255.255");
        }
        return (String[]) t.toArray(new String[t.size()]);
    }

    private volatile String txResult;

    /*
     * Маяк: раз в секунду широковещательный пакет со своим адресом.
     *
     * Он проверяет обратное направление и не требует от человека ничего
     * набирать. Если телефон, слушая порт 45455, видит маяк — провод исправен
     * в обе стороны, и остаётся только понять, куда уходят его собственные
     * пакеты. Если не видит — связи нет и в ту сторону, а значит дело не в
     * приложении на телефоне.
     *
     * Содержимое — собственный адрес ГУ. Тот же пакет позже избавит от
     * ручного ввода: приложению-мосту останется услышать маяк и ответить.
     */
    private volatile boolean beacon;
    private Thread beaconThread;

    public boolean isBeaconOn() {
        return beacon;
    }

    public String getBeaconState() {
        return beacon ? "ВКЛ — раз в секунду широковещательно" : "ВЫКЛ";
    }

    public void toggleBeacon() {
        if (beacon) {
            beacon = false;
            beaconThread = null;
            return;
        }
        beacon = true;
        beaconThread = new Thread(new Runnable() {
            public void run() {
                DatagramSocket s = null;
                try {
                    s = new DatagramSocket();
                    s.setBroadcast(true);
                    while (beacon) {
                        String me = com.q50gtr.plus.diag.TransportProbe
                                .ifaceAddress("usb0");
                        String[] t = probeTargets();
                        byte[] msg = ("Q50GTR BEACON " + (me == null ? "?" : me)
                                + " PORT " + PORT + "\n").getBytes("UTF-8");
                        for (int i = 0; i < t.length; i++) {
                            int sp = t[i].indexOf(' ');
                            if (!t[i].startsWith("шир")) {
                                continue;
                            }
                            try {
                                s.send(new DatagramPacket(msg, msg.length,
                                        InetAddress.getByName(t[i].substring(sp + 1)),
                                        PORT));
                            } catch (Throwable ignored) {
                            }
                        }
                        Thread.sleep(1000L);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "маяк остановлен: " + t);
                } finally {
                    beacon = false;
                    if (s != null) {
                        try {
                            s.close();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        }, "q50-beacon");
        beaconThread.setDaemon(true);
        beaconThread.start();
    }

    /** Последний результат пробы передачи, для диагностического экрана. */
    public String getTxResult() {
        return txResult == null ? "не запускалась" : txResult;
    }

    /** Запускалась ли проба передачи в этом запуске. */
    public boolean isTxTried() {
        return txResult != null;
    }

    /** Причина отказа без пакета и стека: на экране важна суть. */
    private static String reason(Throwable t) {
        String m = t.getMessage();
        String n = t.getClass().getName();
        int d = n.lastIndexOf('.');
        return (d < 0 ? n : n.substring(d + 1)) + (m == null ? "" : " " + m);
    }

    /** Счётчик переданных пакетов интерфейса, или -1 если не прочитать. */
    private long txPackets(String name) {
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(new java.io.FileReader(
                    "/sys/class/net/" + name + "/statistics/tx_packets"), 64);
            String ln = r.readLine();
            return ln == null ? -1L : Long.parseLong(ln.trim());
        } catch (Throwable t) {
            return -1L;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /*
     * Прирост принятых кадров за последние секунды.
     *
     * Снимок счётчика на фотографии не отвечает на главный вопрос: растёт он
     * или стоит. А именно этим различаются две оставшиеся причины, по которым
     * пакет с телефона не доходит. Растёт — кадры до ГУ долетают и гибнут
     * выше, в фильтре или сокете. Стоит — до провода не доходит ничего, и
     * телефон отправляет их куда-то мимо (например, в Wi-Fi, если тот остался
     * сетью по умолчанию). Поэтому счётчик запоминается с отметкой времени, а
     * на экран идёт разница.
     */
    private volatile long rxSampleVal = -1L;
    private volatile long rxSampleAtMs;
    private volatile long rxDelta;
    private volatile long rxDeltaSecs;

    private void sampleRx(long rx) {
        long now = System.currentTimeMillis();
        if (rxSampleVal < 0) {
            rxSampleVal = rx;
            rxSampleAtMs = now;
            return;
        }
        long dt = now - rxSampleAtMs;
        if (dt < 10000L) {
            return;
        }
        rxDelta = rx - rxSampleVal;
        rxDeltaSecs = dt / 1000L;
        rxSampleVal = rx;
        rxSampleAtMs = now;
    }

    private String rxGrowth() {
        if (rxDeltaSecs == 0) {
            return "счёт идёт";
        }
        return (rxDelta > 0 ? "+" : "") + rxDelta + " за " + rxDeltaSecs + "с";
    }

    private static long parseCounter(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** Адреса, на которые можно слать: их и надо вбить в телефоне. */
    public String getLocalAddresses() {
        StringBuilder b = new StringBuilder();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e != null && e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                Enumeration<InetAddress> a = ni.getInetAddresses();
                while (a.hasMoreElements()) {
                    InetAddress ia = a.nextElement();
                    if (ia.isLoopbackAddress() || ia.getHostAddress().indexOf(':') >= 0) {
                        continue;
                    }
                    if (b.length() > 0) {
                        b.append("  ");
                    }
                    // С именем интерфейса и состоянием: адрес может висеть и
                    // на опущенном интерфейсе, и тогда всё выглядит
                    // настроенным, а связи нет.
                    b.append(ni.getName()).append('=').append(ia.getHostAddress());
                    if (!com.q50gtr.plus.diag.TransportProbe.isIfaceUp(ni.getName())) {
                        b.append("(DOWN)");
                    }
                }
            }
        } catch (Throwable t) {
            return "не определить: " + t;
        }
        return b.length() == 0 ? "сети нет" : b.toString();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "q50-bridge");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
        DatagramSocket s = socket;
        socket = null;
        if (s != null) {
            s.close();
        }
        worker = null;
    }

    public boolean isConnected() {
        return packets > 0 && System.currentTimeMillis() - lastRxMs < STALE_AFTER_MS;
    }

    private void loop() {
        try {
            socket = new DatagramSocket(PORT);
            Log.i(TAG, "слушаю UDP " + PORT + " на " + getLocalAddresses());
        } catch (Throwable t) {
            lastError = "не занять порт " + PORT + ": " + t;
            Log.w(TAG, lastError);
            return;
        }
        byte[] buf = new byte[2048];
        while (running) {
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(p);
            } catch (Throwable t) {
                if (running) {
                    lastError = "приём: " + t;
                }
                break;
            }
            try {
                parse(new String(p.getData(), 0, p.getLength(), "UTF-8"));
                lastSender = p.getAddress() == null ? "?" : p.getAddress().getHostAddress();
                packets++;
                lastRxMs = System.currentTimeMillis();
            } catch (Throwable t) {
                lastError = "разбор: " + t;
            }
        }
    }

    private void parse(String body) {
        String[] lines = body.split("\n");
        synchronized (lock) {
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i].trim();
                int eq = ln.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = ln.substring(0, eq).trim().toUpperCase();
                float v;
                try {
                    v = Float.parseFloat(ln.substring(eq + 1).trim());
                } catch (Throwable t) {
                    continue;
                }
                if (v != v || v == Float.POSITIVE_INFINITY || v == Float.NEGATIVE_INFINITY) {
                    continue;
                }
                if ("TEST_COUNTER".equals(key)) {
                    long v2 = (long) v;
                    if (testCounter >= 0 && v2 > testCounter + 1) {
                        testGaps += v2 - testCounter - 1;
                    }
                    testCounter = v2;
                    continue;
                }
                if ("TEST_MS".equals(key)) {
                    // Часы телефона и ГУ не синхронизированы, поэтому сдвиг
                    // сам по себе ничего не значит — значим его РОСТ: он
                    // показывает, что пакеты отстают.
                    testLatencyMs = System.currentTimeMillis() - (long) v;
                    continue;
                }
                float[] cell = pending.get(key);
                if (cell == null) {
                    cell = new float[1];
                    pending.put(key, cell);
                }
                cell[0] = v;
            }
        }
    }

    public void poll(VehicleData d, long nowMs) {
        synchronized (lock) {
            if (pending.isEmpty()) {
                return;
            }
            java.util.Iterator<java.util.Map.Entry<String, float[]>> it =
                    pending.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<String, float[]> e = it.next();
                String key = baseKey(e.getKey());
                Channel c = channelFor(d, key);
                float v = e.getValue()[0] * scaleFor(e.getKey());
                if (c != null && sane(key, v)) {
                    c.setLive(v, NAME, nowMs);
                }
            }
            pending.clear();
        }
    }

    /**
     * Грубая проверка на физический смысл. Приёмник открыт в сеть, и на
     * приборы автомобиля не должно попадать то, что не может быть правдой.
     */
    private static boolean sane(String key, float v) {
        if ("RPM".equals(key)) {
            return v >= 0f && v <= 9000f;
        }
        if ("SPEED".equals(key)) {
            return v >= 0f && v <= 400f;
        }
        if (key.startsWith("AFR")) {
            return v >= 5f && v <= 25f;
        }
        if (key.indexOf("TEMP") >= 0) {
            return v >= -60f && v <= 200f;
        }
        if (key.startsWith("BOOST")) {
            return v >= -1.5f && v <= 4f;
        }
        if (key.startsWith("AIR_")) {
            // Подушки держат единицы бар, ресивер до ~14. Всё, что выше, —
            // чужие единицы или мусор, и на приборку это попадать не должно.
            return v >= 0f && v <= 16f;
        }
        return v > -100000f && v < 100000f;
    }

    private static Channel channelFor(VehicleData d, String key) {
        if ("RPM".equals(key)) {
            return d.rpm;
        }
        if ("SPEED".equals(key)) {
            return d.speed;
        }
        if ("BOOST".equals(key) || "BOOST_ACTUAL".equals(key)) {
            return d.boostActual;
        }
        if ("BOOST_TARGET".equals(key)) {
            return d.boostTarget;
        }
        if ("AFR_B1".equals(key)) {
            return d.afrB1;
        }
        if ("AFR_B2".equals(key)) {
            return d.afrB2;
        }
        if ("IGNITION".equals(key) || "IGN_TIMING".equals(key)) {
            return d.ignitionTiming;
        }
        if ("KNOCK_RETARD".equals(key)) {
            return d.knockRetard;
        }
        if (key.length() == 3 && key.startsWith("KI")) {
            int n = key.charAt(2) - '1';
            if (n >= 0 && n < d.knockIndex.length) {
                return d.knockIndex[n];
            }
            return null;
        }
        if ("HPFP".equals(key) || "HPFP_ACTUAL".equals(key)) {
            return d.hpfpActual;
        }
        if ("HPFP_TARGET".equals(key)) {
            return d.hpfpTarget;
        }
        if ("STFT_B1".equals(key)) {
            return d.stftB1;
        }
        if ("STFT_B2".equals(key)) {
            return d.stftB2;
        }
        if ("LTFT_B1".equals(key)) {
            return d.ltftB1;
        }
        if ("LTFT_B2".equals(key)) {
            return d.ltftB2;
        }
        if ("THROTTLE".equals(key)) {
            return d.throttle;
        }
        if ("COOLANT_TEMP".equals(key)) {
            return d.coolantTemp;
        }
        if ("OIL_TEMP".equals(key)) {
            return d.oilTemp;
        }
        if ("INTAKE_TEMP".equals(key)) {
            return d.intakeTemp;
        }
        if ("TRANS_TEMP".equals(key)) {
            return d.transmissionTemp;
        }
        if ("VOLTAGE".equals(key)) {
            return d.batteryVoltage;
        }
        // Пневмоподвеска Air Lift 3H: четыре угла и ресивер. Экран ШАССИ под
        // них уже нарисован и ждёт данных.
        if ("AIR_FL".equals(key)) {
            return d.airFrontLeft;
        }
        if ("AIR_FR".equals(key)) {
            return d.airFrontRight;
        }
        if ("AIR_RL".equals(key)) {
            return d.airRearLeft;
        }
        if ("AIR_RR".equals(key)) {
            return d.airRearRight;
        }
        if ("AIR_TANK".equals(key)) {
            return d.airTank;
        }
        return null;
    }

    /**
     * Множитель к сырому значению. Air Lift считает в psi, а приборка — в
     * барах, и перепутать их легко: 36 psi это 2.5 бара, а 36 бар — это
     * разорванная подушка. Поэтому у давлений два имени ключа, и единица
     * задаётся именем, а не догадкой на приёмной стороне.
     */
    private static float scaleFor(String key) {
        return key.endsWith("_PSI") ? 0.0689476f : 1f;
    }

    /** Имя канала без суффикса единицы. */
    private static String baseKey(String key) {
        return key.endsWith("_PSI") ? key.substring(0, key.length() - 4) : key;
    }
}
