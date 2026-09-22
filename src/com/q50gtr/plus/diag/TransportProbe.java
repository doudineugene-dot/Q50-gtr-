package com.q50gtr.plus.diag;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

/**
 * Глубокая разведка транспортов головного устройства. СТРОГО НА ЧТЕНИЕ.
 *
 * Прошлые проверки отвечали на вопрос «видит ли Android Bluetooth» и дали
 * твёрдое «нет». Этот зонд задаёт другие вопросы, на которые ещё не
 * отвечали: что вообще есть в ядре и в системе — контроллеры USB, реестр
 * служб целиком (а не отфильтрованный по слову bluetooth), модули ядра,
 * сетевые устройства, root.
 *
 * Смысл в том, чтобы отличить «Android не умеет» от «железа нет». Первое
 * иногда обходится, второе — никогда.
 *
 * Ни одна команда здесь ничего не меняет: только чтение файлов и вывод
 * перечисляющих утилит. Прошивка не трогается, штатные службы Bluetooth и
 * телефонии не останавливаются.
 */
public final class TransportProbe {

    private static final String TAG = "Q50GTR/TRANSPORT";
    /** Потолок строк на один источник, чтобы отчёт оставался читаемым. */
    private static final int MAX_LINES = 60;

    private final StringBuilder report = new StringBuilder();
    private final Context context;

    private boolean rootPresent;
    private boolean rootWorks;
    private boolean usbHost;
    private boolean usbGadget;
    private boolean anyHci;
    private int services;
    private String diskKey = "";
    private final java.util.List<String> hits = new java.util.ArrayList<String>();
    private final java.util.List<String> samples = new java.util.ArrayList<String>();
    /** Полный путь к rndis_host.ko, если он нашёлся на диске. */
    private volatile String rndisPath;
    private volatile String loadResult;

    private static String canon(String p) {
        try {
            return new File(p).getCanonicalPath();
        } catch (Throwable t) {
            return p;
        }
    }

    public TransportProbe(Context context) {
        this.context = context;
    }

    public String getReport() {
        return report.toString();
    }

    public boolean isRootWorks() {
        return rootWorks;
    }

    public boolean hasUsbHost() {
        return usbHost;
    }

    public boolean hasUsbGadget() {
        return usbGadget;
    }

    public boolean hasHci() {
        return anyHci;
    }

    /** Короткая сводка для экрана: один взгляд — один ответ. */
    public String getDiskKey() {
        return diskKey;
    }

    public String getRndisPath() {
        return rndisPath;
    }

    public String getLoadResult() {
        return loadResult;
    }

    public boolean isRndisLoaded() {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader("/proc/modules"), 4096);
            String ln;
            while ((ln = r.readLine()) != null) {
                if (ln.startsWith("rndis_host")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            close(r);
        }
        return false;
    }

    /** Есть ли интерфейс с таким именем в ядре. */
    public static boolean hasIface(String name) {
        return new File("/sys/class/net/" + name).exists();
    }

    /**
     * Поднят ли интерфейс. Флаги ядра, бит 0x1 — IFF_UP. Именно этого
     * признака не хватало: адрес на интерфейсе может висеть и при DOWN, и
     * тогда всё выглядит настроенным, а ядро молчит даже на ARP.
     */
    public static boolean isIfaceUp(String name) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(
                    new FileReader("/sys/class/net/" + name + "/flags"), 64);
            String ln = r.readLine();
            if (ln == null) {
                return false;
            }
            ln = ln.trim();
            if (ln.startsWith("0x") || ln.startsWith("0X")) {
                ln = ln.substring(2);
            }
            return (Long.parseLong(ln, 16) & 1L) != 0L;
        } catch (Throwable t) {
            return false;
        } finally {
            close(r);
        }
    }

    /**
     * Подробности канала: несущая, состояние линка, MTU, MAC и счётчики
     * ошибок передачи.
     *
     * IFF_UP говорит только о том, что интерфейс включили административно.
     * Кадры уходят в провод лишь при поднятой несущей: драйвер rndis_host
     * на неродном гаджете может принимать и при этом не звать
     * netif_carrier_on, и тогда снаружи всё выглядит настроенным, а TX
     * стоит на нуле. Отличить это от «нет маршрута» можно только здесь.
     */
    public static String linkDetails(String name) {
        String base = "/sys/class/net/" + name + "/";
        StringBuilder b = new StringBuilder();
        b.append("carrier=").append(readOne(base + "carrier"));
        b.append(" oper=").append(readOne(base + "operstate"));
        b.append(" mtu=").append(readOne(base + "mtu"));
        b.append(" mac=").append(readOne(base + "address"));
        b.append(" txerr=").append(readOne(base + "statistics/tx_errors"));
        b.append(" txdrop=").append(readOne(base + "statistics/tx_dropped"));
        b.append(" rxerr=").append(readOne(base + "statistics/rx_errors"));
        return b.toString();
    }

    /** Первая строка файла из /sys или /proc, или «?» если не прочитать. */
    private static String readOne(String path) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path), 64);
            String ln = r.readLine();
            return ln == null ? "?" : ln.trim();
        } catch (Throwable t) {
            return "?";
        } finally {
            close(r);
        }
    }

    /**
     * Таблица маршрутов человеческими адресами. /proc/net/route хранит их
     * шестнадцатеричными и в обратном порядке байт, поэтому глазами её не
     * прочитать — а именно отсутствие маршрута на 192.168.42.0/24 объяснило
     * бы, почему отправка не уходит никуда.
     */
    public static String routes() {
        BufferedReader r = null;
        StringBuilder b = new StringBuilder();
        try {
            r = new BufferedReader(new FileReader("/proc/net/route"), 4096);
            String ln = r.readLine(); // заголовок
            while ((ln = r.readLine()) != null) {
                String[] f = ln.trim().split("\\s+");
                if (f.length < 8) {
                    continue;
                }
                if (b.length() > 0) {
                    b.append("  ");
                }
                b.append(f[0]).append(':').append(hexIp(f[1]))
                        .append('/').append(maskBits(f[7]));
                if (!"00000000".equalsIgnoreCase(f[2])) {
                    b.append("→").append(hexIp(f[2]));
                }
            }
        } catch (Throwable t) {
            return "не прочитать: " + t;
        } finally {
            close(r);
        }
        return b.length() == 0 ? "маршрутов нет" : b.toString();
    }

    /** Таблица ARP: ответил ли телефон на запрос адреса. */
    public static String arp() {
        BufferedReader r = null;
        StringBuilder b = new StringBuilder();
        try {
            r = new BufferedReader(new FileReader("/proc/net/arp"), 4096);
            String ln = r.readLine(); // заголовок
            while ((ln = r.readLine()) != null) {
                String[] f = ln.trim().split("\\s+");
                if (f.length < 6) {
                    continue;
                }
                if (b.length() > 0) {
                    b.append("  ");
                }
                b.append(f[0]).append('=').append(f[3]).append('@').append(f[5]);
            }
        } catch (Throwable t) {
            return "не прочитать: " + t;
        } finally {
            close(r);
        }
        return b.length() == 0 ? "ARP пуст" : b.toString();
    }

    /** Little-endian hex из /proc/net/route в обычную запись адреса. */
    private static String hexIp(String hex) {
        try {
            long v = Long.parseLong(hex, 16);
            return (v & 0xFF) + "." + ((v >> 8) & 0xFF) + "."
                    + ((v >> 16) & 0xFF) + "." + ((v >> 24) & 0xFF);
        } catch (Throwable t) {
            return hex;
        }
    }

    /** Длина префикса из шестнадцатеричной маски. */
    private static String maskBits(String hex) {
        try {
            long v = Long.parseLong(hex, 16);
            int n = 0;
            while (v != 0) {
                n += (int) (v & 1L);
                v >>>= 1;
            }
            return Integer.toString(n);
        } catch (Throwable t) {
            return hex;
        }
    }

    /** Адрес интерфейса, или null если не настроен. */
    public static String ifaceAddress(String name) {
        try {
            java.net.NetworkInterface ni = java.net.NetworkInterface.getByName(name);
            if (ni == null) {
                return null;
            }
            java.util.Enumeration<java.net.InetAddress> a = ni.getInetAddresses();
            while (a.hasMoreElements()) {
                java.net.InetAddress ia = a.nextElement();
                if (!ia.isLoopbackAddress() && ia.getHostAddress().indexOf(':') < 0) {
                    return ia.getHostAddress();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Настраивает usb0 — интерфейс, который ядро создаёт при подключении
     * телефона в режиме USB-модема. ТОЛЬКО ПО ЯВНОМУ НАЖАТИЮ.
     *
     * Адрес задаётся статически, а не по DHCP. Android при раздаче по USB
     * работает в подсети 192.168.42.0/24 и сам занимает .129, так что адрес
     * предсказуем; а DHCP-клиент на этой прошивке может отсутствовать или
     * подвиснуть, и тогда непонятно, что пошло не так. Статика отрабатывает
     * мгновенно и даёт один и тот же адрес — его можно прямо назвать в
     * инструкции.
     *
     * Команда идёт дважды: через toolbox и через busybox. Синтаксис ifconfig
     * у них различается версиями, а лишний повтор с теми же значениями
     * безвреден.
     *
     * Действие временное: ничего на диске не меняется, после выключения
     * зажигания интерфейс исчезнет вместе с подключением.
     */
    public String configureUsb0() {
        if (!hasIface("usb0")) {
            loadResult = "usb0 ещё нет — подключите телефон и включите USB-модем";
            return loadResult;
        }
        // Настроенным интерфейс считается только поднятым. Раньше хватало
        // адреса, и повторное нажатие рапортовало «уже настроен» об
        // интерфейсе в состоянии DOWN, то есть мешало себя же починить.
        String already = ifaceAddress("usb0");
        if (already != null && isIfaceUp("usb0")) {
            loadResult = "usb0 уже настроен: " + already
                    + "  " + linkDetails("usb0");
            return loadResult;
        }
        // Поднимать интерфейс НУЖНО ОТДЕЛЬНОЙ командой. Прошлый вариант
        // задавал адрес и «up» одной строкой, toolbox съел адрес и
        // проигнорировал флаг: на машине это дало usb0 с адресом, но в
        // состоянии DOWN — кадры приходили, а ядро не отвечало даже на ARP
        // (TX 0). Поэтому теперь up идёт и до адреса, и после, разными
        // инструментами: синтаксис у toolbox, busybox и netcfg разный, а
        // повтор с теми же значениями безвреден.
        String cmds =
                "ifconfig usb0 up\n"
                + "netcfg usb0 up\n"
                + "busybox ifconfig usb0 up\n"
                + "ifconfig usb0 " + USB0_IP + " netmask 255.255.255.0\n"
                + "busybox ifconfig usb0 " + USB0_IP + " netmask 255.255.255.0\n"
                + "ifconfig usb0 up\n"
                + "busybox ifconfig usb0 up\n"
                + "busybox route add -net 192.168.42.0 netmask 255.255.255.0 dev usb0\n"
                + "ifconfig usb0\n";
        String out = runAsRoot(cmds);
        String addr = ifaceAddress("usb0");
        // Адреса мало: он был и в прошлый раз. Признаком служит состояние.
        boolean up = isIfaceUp("usb0");
        loadResult = (addr != null && up ? "usb0 ГОТОВ: " : "usb0 НЕ ГОТОВ: ")
                + "addr=" + (addr == null ? "нет" : addr)
                + " state=" + (up ? "UP" : "DOWN")
                + "  " + linkDetails("usb0")
                + "  " + (out.length() == 0 ? "" : out);
        Log.i(TAG, loadResult);
        return loadResult;
    }

    /** Адрес, который берёт себе ГУ. Телефон при раздаче занимает .129. */
    public static final String USB0_IP = "192.168.42.2";

    /** Выполняет команды от root через stdin su и возвращает весь вывод. */
    private String runAsRoot(String script) {
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("su");
            java.io.OutputStream os = p.getOutputStream();
            os.write(script.getBytes("UTF-8"));
            os.write("exit\n".getBytes("UTF-8"));
            os.flush();
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()), 2048);
            BufferedReader e = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()), 2048);
            String ln;
            while ((ln = r.readLine()) != null) {
                out.append(ln).append(' ');
            }
            while ((ln = e.readLine()) != null) {
                out.append(ln).append(' ');
            }
            p.waitFor();
            close(r);
            close(e);
        } catch (Throwable t) {
            out.append("не выполнить: ").append(t);
        }
        return out.toString().trim();
    }

    /**
     * Поднимает драйвер rndis_host, которым ядро подхватывает телефон в
     * режиме USB-модема. ТОЛЬКО ПО ЯВНОМУ НАЖАТИЮ — сам по себе этот метод
     * не вызывается ниоткуда.
     *
     * Модуль штатный, из прошивки этого же ГУ, и привязывается только к
     * USB-устройству телефона: штатных систем машины он не касается.
     * Действие временное — после перезагрузки ГУ модуль выгрузится сам,
     * потому что ничего на диске мы не меняем.
     *
     * Команда уходит в stdin процесса su, а не аргументом: так её понимают
     * все известные реализации su, а разбор аргументов у них разный.
     */
    public String loadRndis() {
        if (rndisPath == null) {
            loadResult = "rndis_host.ko на диске не найден";
            return loadResult;
        }
        if (isRndisLoaded()) {
            loadResult = "rndis_host уже загружен";
            return loadResult;
        }
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("su");
            java.io.OutputStream os = p.getOutputStream();
            os.write(("insmod " + rndisPath + "\n").getBytes("UTF-8"));
            os.write("exit\n".getBytes("UTF-8"));
            os.flush();
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()), 2048);
            BufferedReader e = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()), 2048);
            String ln;
            while ((ln = r.readLine()) != null) {
                out.append(ln).append(' ');
            }
            while ((ln = e.readLine()) != null) {
                out.append(ln).append(' ');
            }
            p.waitFor();
            close(r);
            close(e);
        } catch (Throwable t) {
            loadResult = "insmod не выполнить: " + t;
            Log.w(TAG, loadResult);
            return loadResult;
        }
        boolean ok = isRndisLoaded();
        loadResult = (ok ? "ЗАГРУЖЕН: " : "НЕ ЗАГРУЗИЛСЯ: ")
                + (out.length() == 0 ? "(без сообщений)" : out.toString().trim());
        Log.i(TAG, loadResult);
        return loadResult;
    }

    public String summary() {
        return "root=" + (rootWorks ? "ДА" : (rootPresent ? "есть su, не даёт" : "нет"))
                + "  HCI=" + (anyHci ? "ЕСТЬ" : "нет")
                + "  USB host=" + (usbHost ? "ЕСТЬ" : "нет")
                + "  USB gadget=" + (usbGadget ? "ЕСТЬ" : "нет")
                + "  служб=" + services;
    }

    public void run() {
        line("== РАЗВЕДКА ТРАНСПОРТОВ (только чтение) ==");
        int mark = report.length();
        kernel();
        rootCheck();
        hciCheck();
        usbCheck();
        netCheck();
        serviceCheck();
        classCheck();
        ttyCheck();
        line("== КОНЕЦ ==");
        // Итог ставится В НАЧАЛО отчёта: длинные строки на 800x480 обрезаются
        // справа, и решающий вывод не должен зависеть от того, влез он в
        // ширину экрана или нет.
        report.insert(mark, "ИТОГ: " + summary() + "\n"
                + "ИТОГ: модули на диске -> "
                + (diskKey.length() == 0 ? "RNDIS/BTUSB НЕТ" : diskKey) + "\n");
    }

    private void kernel() {
        line("-- ядро --");
        String v = firstLine("/proc/version");
        line("  " + (v == null ? "/proc/version не прочитать" : v));
        int mods = countLines("/proc/modules");
        line("  загружаемых модулей: " + (mods < 0 ? "/proc/modules не прочитать" : "" + mods));
        // ВСЕ модули, а не первые двенадцать. Прошлый замер обрезал список,
        // и именно в отрезанной части могли лежать btusb и rndis_host —
        // ровно те, от которых зависит, оживёт ли USB-путь.
        modules();
    }

    /**
     * Перечисляет модули одними именами, по нескольку в строке: полный
     * список из 34 штук в столбик не влезает на экран, а важны в нём только
     * имена. Отдельно выделяются те, что решают судьбу транспортов.
     */
    private void modules() {
        BufferedReader r = null;
        StringBuilder b = new StringBuilder("  ");
        StringBuilder key = new StringBuilder();
        try {
            r = new BufferedReader(new FileReader("/proc/modules"), 4096);
            String ln;
            int perLine = 0;
            while ((ln = r.readLine()) != null) {
                int sp = ln.indexOf(' ');
                String name = sp > 0 ? ln.substring(0, sp) : ln;
                b.append(name).append(' ');
                if (isKeyModule(name)) {
                    key.append(name).append(' ');
                }
                if (++perLine >= 6) {
                    line(b.toString());
                    b = new StringBuilder("  ");
                    perLine = 0;
                }
            }
            if (b.length() > 2) {
                line(b.toString());
            }
        } catch (Throwable t) {
            line("  /proc/modules не прочитать");
        } finally {
            close(r);
        }
        line("  ЗАГРУЖЕНЫ РЕШАЮЩИЕ: " + (key.length() == 0
                ? "btusb/rndis_host/usbnet/cdc_* НЕТ" : key.toString().trim()));
        moduleFiles();
    }

    /**
     * Модули, лежащие на диске, но не загруженные.
     *
     * Телефон в режиме USB-модема обычно представляется как RNDIS, и
     * подхватит его только rndis_host. Если этого модуля нет в памяти, но
     * есть файлом — путь остаётся открытым, просто модуль не поднят. Если
     * файла нет вовсе — ядро телефон не увидит, и USB-модем отпадает.
     *
     * Здесь только перечисление файлов. Ничего не загружается: insmod
     * меняет состояние системы, а на это нужно отдельное разрешение.
     */
    private void moduleFiles() {
        String[] dirs = {"/system/lib/modules", "/lib/modules", "/system/modules"};
        StringBuilder seen = new StringBuilder();
        boolean found = false;
        for (int d = 0; d < dirs.length; d++) {
            File[] f = new File(dirs[d]).listFiles();
            if (f == null || f.length == 0) {
                continue;
            }
            if (seen.indexOf(canon(dirs[d])) >= 0) {
                continue;   // /lib/modules часто ссылка на /system/lib/modules
            }
            seen.append(canon(dirs[d])).append(' ');
            found = true;
            StringBuilder key = new StringBuilder();
            int[] count = new int[1];
            // Модули лежат не в самом каталоге, а в подкаталоге с именем
            // версии ядра. Прошлый замер перечислил верхний уровень, увидел
            // там одну папку и объявил, что модулей нет. Теперь обход
            // рекурсивный — иначе это проверка папки снаружи, а не модулей.
            walk(new File(dirs[d]), 0, key, count);
            line("  модулей на диске: " + count[0]);
            for (int i = 0; i < samples.size(); i++) {
                line("    пример: " + samples.get(i));
            }
            // Не «да/нет», а сами имена: список из 94 штук всё равно не
            // прочитать, а по именам видно и точное написание, и близкие
            // варианты, если модуль назван иначе.
            line("  RNDIS/BT/NET на диске:");
            for (int i = 0; i < hits.size(); i++) {
                line("    " + hits.get(i));
            }
            if (hits.isEmpty()) {
                line("    ничего похожего не найдено");
            }
            diskKey = key.toString().trim();
        }
        if (!found) {
            line("  каталогов с модулями не найдено");
        }
    }

    /**
     * Рекурсивный обход каталога модулей.
     *
     * Глубина была ограничена тремя уровнями, и это дало ложный ответ:
     * модули ядра лежат по пути
     * /system/lib/modules/<версия>/kernel/drivers/net/usb/cdc_ether.ko — это
     * шесть уровней. Обход останавливался на третьем и сообщал, что
     * cdc_ether на диске нет, тогда как он в это же время был загружен.
     * Предел поднят до глубины, заведомо покрывающей дерево drivers.
     */
    private void walk(File dir, int depth, StringBuilder key, int[] count) {
        if (depth > 10 || count[0] > 4000) {
            return;
        }
        File[] f = dir.listFiles();
        if (f == null) {
            return;
        }
        for (int i = 0; i < f.length; i++) {
            if (f[i].isDirectory()) {
                walk(f[i], depth + 1, key, count);
                continue;
            }
            String nm = f[i].getName();
            if (!nm.endsWith(".ko")) {
                continue;
            }
            count[0]++;
            String bare = nm.substring(0, nm.length() - 3);
            if (isKeyModule(bare)) {
                key.append(bare).append(' ');
            }
            if (samples.size() < 3) {
                samples.add(f[i].getPath());
            }
            String low = bare.toLowerCase();
            if (hits.size() < 24 && (low.indexOf("rndis") >= 0 || low.indexOf("btusb") >= 0
                    || low.indexOf("usbnet") >= 0 || low.indexOf("cdc") >= 0
                    || low.startsWith("bt_") || low.indexOf("bluetooth") >= 0
                    || low.indexOf("ecm") >= 0 || low.indexOf("ncm") >= 0)) {
                // Полный путь, а не имя: по нему видно, куда вообще дотянулся
                // обход, и не обрезан ли он снова.
                hits.add(f[i].getPath());
            }
            if ("rndis_host".equals(bare)) {
                rndisPath = f[i].getPath();
            }
        }
    }

    /** Модули, от которых зависят пути USB и внешнего BLE-адаптера. */
    private static boolean isKeyModule(String n) {
        return "btusb".equals(n) || "rndis_host".equals(n) || "usbnet".equals(n)
                || "rndis_wlan".equals(n) || "cdc_subset".equals(n) || "asix".equals(n)
                || "ax88179_178a".equals(n) || "r8152".equals(n)
                || "cdc_ether".equals(n) || "cdc_ncm".equals(n) || "cdc_acm".equals(n)
                || "hci_uart".equals(n) || "bluetooth".equals(n) || "bnep".equals(n)
                || "usbserial".equals(n) || "rndis_wlan".equals(n);
    }

    /**
     * Есть ли в этой прошивке классы USB и Ethernet. Службы в реестре
     * найдены (IUsbManager, IEthernetManager), но штатный USB Host API
     * появился только в API 12 — а это вендорская сборка, и она вполне могла
     * принести классы с собой. Проверяется рефлексией, потому что
     * скомпилировать обращение к ним под API 10 нечем.
     */
    private void classCheck() {
        line("-- классы за пределами API 10 --");
        String[] names = {
                "android.hardware.usb.UsbManager",
                "android.hardware.usb.UsbDevice",
                "android.hardware.usb.UsbDeviceConnection",
                "android.net.ethernet.EthernetManager",
                "android.bluetooth.BluetoothAdapter",
        };
        for (int i = 0; i < names.length; i++) {
            boolean ok;
            try {
                Class.forName(names[i]);
                ok = true;
            } catch (Throwable t) {
                ok = false;
            }
            line("  " + names[i] + " -> " + (ok ? "ЕСТЬ" : "нет"));
        }
    }

    /**
     * Root нужен не сам по себе: он определяет, можно ли вообще заглянуть
     * туда, куда обычному приложению хода нет. Проверяется чтением, а не
     * изменением чего-либо.
     */
    private void rootCheck() {
        line("-- root --");
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su"};
        for (int i = 0; i < paths.length; i++) {
            if (new File(paths[i]).exists()) {
                rootPresent = true;
                line("  ЕСТЬ " + paths[i]);
            }
        }
        if (!rootPresent) {
            line("  su не найден");
            return;
        }
        String id = exec1("su -c id");
        if (id == null) {
            line("  su -c id: не выполнить");
            return;
        }
        line("  su -c id -> " + id);
        rootWorks = id.indexOf("uid=0") >= 0;
    }

    private void hciCheck() {
        line("-- контроллер Bluetooth в ядре --");
        // Каталог /sys/class/bluetooth существует в любом образе Android.
        // Значение имеет только его содержимое: там перечислены реальные
        // HCI-контроллеры. Пусто — контроллера нет физически.
        File[] hci = new File("/sys/class/bluetooth").listFiles();
        if (hci == null) {
            line("  /sys/class/bluetooth не прочитать");
        } else if (hci.length == 0) {
            line("  /sys/class/bluetooth ПУСТ -> HCI-контроллеров нет");
        } else {
            for (int i = 0; i < hci.length; i++) {
                anyHci = true;
                line("  HCI: " + hci[i].getName());
            }
        }
        grepFile("/proc/devices", "blue", "  /proc/devices: ");
        if (rootWorks) {
            exec("su -c dmesg", 8, "bluetooth|hci|bcm|btusb", "  dmesg: ");
        }
    }

    /**
     * USB. Host-контроллер нужен для внешнего адаптера, gadget — для режима
     * устройства. Android USB Host API появился только в API 12, здесь API
     * 10, но наличие самого контроллера — отдельный факт, и знать его стоит:
     * он отделяет «нет программного интерфейса» от «нет железа».
     */
    private void usbCheck() {
        line("-- USB --");
        File[] devs = new File("/sys/bus/usb/devices").listFiles();
        if (devs == null) {
            line("  /sys/bus/usb/devices не прочитать -> USB-шины не видно");
        } else if (devs.length == 0) {
            line("  /sys/bus/usb/devices пуст");
        } else {
            usbHost = true;
            StringBuilder b = new StringBuilder("  устройства на шине:");
            for (int i = 0; i < devs.length && i < 16; i++) {
                b.append(' ').append(devs[i].getName());
            }
            line(b.toString());
            for (int i = 0; i < devs.length && i < 8; i++) {
                String p = firstLine(devs[i].getPath() + "/product");
                String m = firstLine(devs[i].getPath() + "/manufacturer");
                if (p != null || m != null) {
                    line("    " + devs[i].getName() + ": "
                            + (m == null ? "" : m + " ") + (p == null ? "" : p));
                }
            }
        }
        File[] udc = new File("/sys/class/udc").listFiles();
        if (udc != null && udc.length > 0) {
            usbGadget = true;
            for (int i = 0; i < udc.length; i++) {
                line("  gadget-контроллер: " + udc[i].getName());
            }
        } else {
            line("  /sys/class/udc пуст или недоступен -> режима устройства нет");
        }
        File usbfs = new File("/dev/bus/usb");
        line("  /dev/bus/usb " + (usbfs.exists() ? "ЕСТЬ (usbfs)" : "нет"));
    }

    private void netCheck() {
        line("-- сеть --");
        File[] nets = new File("/sys/class/net").listFiles();
        StringBuilder b = new StringBuilder("  /sys/class/net:");
        if (nets == null || nets.length == 0) {
            b.append(" пусто");
        } else {
            for (int i = 0; i < nets.length; i++) {
                b.append(' ').append(nets[i].getName());
            }
        }
        line(b.toString());
        cat("/proc/net/dev", 8, "  dev: ");
        // Загружены iptable_nat и nf_nat — значит фильтр в системе есть.
        // Правило DROP на входе выглядело бы точно так же, как потерянный
        // маршрут, поэтому таблицу стоит увидеть.
        if (rootWorks) {
            exec("su -c iptables -L INPUT -n", 10, null, "  iptables: ");
        }
    }

    /**
     * Реестр служб ЦЕЛИКОМ. Прошлая проверка фильтровала по слову
     * «bluetooth» и ничего не нашла — но вендорская служба может называться
     * иначе, и тогда фильтр её просто прятал.
     */
    private void serviceCheck() {
        line("-- системные службы (весь список) --");
        services = exec("service list", MAX_LINES, null, "  ");
        line("-- пакеты производителя --");
        exec("pm list packages", 18, "connexis|ygomi|ivi|garage|nissan|infiniti|bt|phone|tel",
                "  ");
    }

    private void ttyCheck() {
        line("-- последовательные порты --");
        File[] tty = new File("/dev").listFiles();
        if (tty == null) {
            line("  /dev не прочитать");
            return;
        }
        StringBuilder b = new StringBuilder("  необычные tty:");
        int n = 0;
        for (int i = 0; i < tty.length && n < 20; i++) {
            String nm = tty[i].getName();
            if (nm.startsWith("tty") && !nm.matches("tty\\d*")) {
                b.append(' ').append(nm);
                n++;
            }
        }
        line(n == 0 ? "  необычных tty нет" : b.toString());
    }

    /* ------------------------------------------------------------------ */

    private String firstLine(String path) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path), 1024);
            return r.readLine();
        } catch (Throwable t) {
            return null;
        } finally {
            close(r);
        }
    }

    private int countLines(String path) {
        BufferedReader r = null;
        int n = 0;
        try {
            r = new BufferedReader(new FileReader(path), 4096);
            while (r.readLine() != null) {
                n++;
            }
            return n;
        } catch (Throwable t) {
            return -1;
        } finally {
            close(r);
        }
    }

    private void cat(String path, int max, String prefix) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path), 4096);
            String ln;
            int n = 0;
            while ((ln = r.readLine()) != null && n < max) {
                line(prefix + ln.trim());
                n++;
            }
        } catch (Throwable ignored) {
        } finally {
            close(r);
        }
    }

    private void grepFile(String path, String needle, String prefix) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path), 4096);
            String ln;
            boolean any = false;
            while ((ln = r.readLine()) != null) {
                if (ln.toLowerCase().indexOf(needle) >= 0) {
                    line(prefix + ln.trim());
                    any = true;
                }
            }
            if (!any) {
                line(prefix + "совпадений с \"" + needle + "\" нет");
            }
        } catch (Throwable t) {
            line(prefix + "не прочитать");
        } finally {
            close(r);
        }
    }

    private String exec1(String cmd) {
        BufferedReader r = null;
        try {
            Process p = Runtime.getRuntime().exec(cmd.split(" "));
            r = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            return r.readLine();
        } catch (Throwable t) {
            return null;
        } finally {
            close(r);
        }
    }

    /** Возвращает число выведенных строк. filter == null — без фильтра. */
    private int exec(String cmd, int max, String filter, String prefix) {
        BufferedReader r = null;
        int n = 0;
        try {
            Process p = Runtime.getRuntime().exec(cmd.split(" "));
            r = new BufferedReader(new InputStreamReader(p.getInputStream()), 4096);
            String ln;
            while ((ln = r.readLine()) != null && n < max) {
                String low = ln.toLowerCase();
                if (filter != null && !matches(low, filter)) {
                    continue;
                }
                line(prefix + ln.trim());
                n++;
            }
        } catch (Throwable t) {
            line(prefix + "[" + cmd + "] не выполнить: " + t);
        } finally {
            close(r);
        }
        return n;
    }

    private static boolean matches(String s, String alternatives) {
        String[] parts = alternatives.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            if (s.indexOf(parts[i]) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private void line(String s) {
        report.append(s).append('\n');
        Log.i(TAG, s);
    }
}
