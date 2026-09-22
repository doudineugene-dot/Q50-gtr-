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
    private static final int MAX_LINES = 40;

    private final StringBuilder report = new StringBuilder();
    private final Context context;

    private boolean rootPresent;
    private boolean rootWorks;
    private boolean usbHost;
    private boolean usbGadget;
    private boolean anyHci;
    private int services;

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
    public String summary() {
        return "root=" + (rootWorks ? "ДА" : (rootPresent ? "есть su, не даёт" : "нет"))
                + "  HCI=" + (anyHci ? "ЕСТЬ" : "нет")
                + "  USB host=" + (usbHost ? "ЕСТЬ" : "нет")
                + "  USB gadget=" + (usbGadget ? "ЕСТЬ" : "нет")
                + "  служб=" + services;
    }

    public void run() {
        line("== РАЗВЕДКА ТРАНСПОРТОВ (только чтение) ==");
        kernel();
        rootCheck();
        hciCheck();
        usbCheck();
        netCheck();
        serviceCheck();
        classCheck();
        ttyCheck();
        line("== КОНЕЦ ==");
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
        boolean found = false;
        for (int d = 0; d < dirs.length; d++) {
            File[] f = new File(dirs[d]).listFiles();
            if (f == null || f.length == 0) {
                continue;
            }
            found = true;
            StringBuilder key = new StringBuilder();
            int[] count = new int[1];
            // Модули лежат не в самом каталоге, а в подкаталоге с именем
            // версии ядра. Прошлый замер перечислил верхний уровень, увидел
            // там одну папку и объявил, что модулей нет. Теперь обход
            // рекурсивный — иначе это проверка папки снаружи, а не модулей.
            walk(new File(dirs[d]), 0, key, count);
            line("  модули на диске (" + dirs[d] + "): найдено " + count[0] + " .ko");
            line("  НА ДИСКЕ РЕШАЮЩИЕ: " + (key.length() == 0
                    ? "rndis_host/btusb/usbnet НЕ НАЙДЕНЫ" : key.toString().trim()));
        }
        if (!found) {
            line("  каталогов с модулями не найдено");
        }
    }

    /** Рекурсивный обход каталога модулей. Глубина ограничена: это не find. */
    private void walk(File dir, int depth, StringBuilder key, int[] count) {
        if (depth > 3) {
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
