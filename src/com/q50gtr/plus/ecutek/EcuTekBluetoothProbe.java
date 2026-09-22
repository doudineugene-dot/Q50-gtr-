package com.q50gtr.plus.ecutek;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Разведка Bluetooth ради адаптера EcuTek EVI. СТРОГО НА ЧТЕНИЕ.
 *
 * Зонд ничего не сопрягает, не подключает и не отправляет. Он отвечает на
 * единственный вопрос, который нельзя решить теорией: виден ли EVI этому
 * головному устройству вообще.
 *
 * Почему это и есть решающая проверка. EcuTek выпускает адаптер в двух
 * исполнениях: EVI-BT на классическом Bluetooth и EVI-BTLE на Bluetooth Low
 * Energy; по документации производителя других отличий между ними нет.
 * Android получил публичный BLE API только в API 18, а здесь API 10 — значит
 * BTLE-исполнение этому ГУ недоступно в принципе, и штатный поиск его даже не
 * покажет. Классический же EVI-BT обязан найтись обычным startDiscovery().
 *
 * Поэтому результат поиска сам по себе определяет исполнение адаптера:
 * нашёлся — Classic, не нашёлся при включённом адаптере и живом EVI — почти
 * наверняка BTLE, и тогда прямой путь с этого ГУ закрыт. Гадать заранее,
 * какой именно адаптер у пользователя, не нужно.
 *
 * Имя устройства EcuTek показывает как «EVI-xxxx», и по нему же его выбирают
 * в системных настройках Bluetooth.
 */
public final class EcuTekBluetoothProbe {

    public static final String TAG = "Q50GTR/ECUTEK_BT";

    /** По этому префиксу EcuTek показывает адаптер в списке Bluetooth. */
    public static final String EVI_PREFIX = "EVI";

    public static final int UNKNOWN = 0;
    public static final int NO_ADAPTER = 1;
    public static final int DISABLED = 2;
    public static final int READY = 3;

    private final Context context;

    private int adapterState = UNKNOWN;
    private String adapterName;
    private String adapterAddress;
    private boolean discoveryRan;
    private boolean discoveryFinished;

    private final List<Found> devices = new ArrayList<Found>();
    private final StringBuilder report = new StringBuilder();
    private String failure;

    /** Одно найденное устройство — всё, что API 10 отдаёт без сопряжения. */
    public static final class Found {
        public String name;
        public String address;
        public int bondState = BluetoothDevice.BOND_NONE;
        public int deviceClass = -1;
        public int majorClass = -1;
        public int rssi = 0;
        public boolean bonded;
        public boolean fromDiscovery;

        public boolean looksLikeEvi() {
            return name != null && name.toUpperCase().startsWith(EVI_PREFIX);
        }

        public String line() {
            StringBuilder b = new StringBuilder();
            b.append(name == null ? "(без имени)" : name);
            b.append("  ").append(address == null ? "??" : address);
            b.append("  bond=").append(bondName(bondState));
            if (deviceClass >= 0) {
                b.append("  class=0x").append(Integer.toHexString(deviceClass));
                b.append("/major=0x").append(Integer.toHexString(majorClass));
            }
            if (rssi != 0) {
                b.append("  rssi=").append(rssi);
            }
            b.append(fromDiscovery ? "  [поиск]" : "  [сопряжённое]");
            if (looksLikeEvi()) {
                b.append("  <-- EVI");
            }
            return b.toString();
        }
    }

    public EcuTekBluetoothProbe(Context context) {
        this.context = context;
    }

    public int getAdapterState() {
        return adapterState;
    }

    public String getFailure() {
        return failure;
    }

    public boolean isDiscoveryFinished() {
        return discoveryFinished;
    }

    public List<Found> getDevices() {
        return devices;
    }

    public String getReport() {
        return report.toString();
    }

    /** Первый похожий на EVI, сопряжённые вперёд: их можно открыть сразу. */
    public Found findEvi() {
        for (int i = 0; i < devices.size(); i++) {
            Found f = devices.get(i);
            if (f.looksLikeEvi() && f.bonded) {
                return f;
            }
        }
        for (int i = 0; i < devices.size(); i++) {
            Found f = devices.get(i);
            if (f.looksLikeEvi()) {
                return f;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */

    /** Снимает состояние адаптера и список сопряжённых. Не ищет в эфире. */
    public void probeAdapter() {
        BluetoothAdapter a;
        try {
            a = BluetoothAdapter.getDefaultAdapter();
        } catch (Throwable t) {
            adapterState = NO_ADAPTER;
            failure = "getDefaultAdapter: " + t;
            line(failure);
            return;
        }
        if (a == null) {
            adapterState = NO_ADAPTER;
            line("Bluetooth adapter: НЕТ (getDefaultAdapter вернул null)");
            return;
        }
        try {
            adapterName = a.getName();
            adapterAddress = a.getAddress();
        } catch (Throwable t) {
            line("имя/адрес адаптера недоступны: " + t);
        }
        boolean on;
        try {
            on = a.isEnabled();
        } catch (Throwable t) {
            on = false;
            line("isEnabled: " + t);
        }
        adapterState = on ? READY : DISABLED;
        line("Bluetooth adapter: ЕСТЬ");
        line("  имя    = " + (adapterName == null ? "?" : adapterName));
        line("  адрес  = " + (adapterAddress == null ? "?" : adapterAddress));
        line("  включён = " + (on ? "ДА" : "НЕТ"));

        Set<BluetoothDevice> bonded = null;
        try {
            bonded = a.getBondedDevices();
        } catch (Throwable t) {
            line("getBondedDevices: " + t);
        }
        line("-- сопряжённые устройства --");
        if (bonded == null || bonded.isEmpty()) {
            line("  (нет)");
        } else {
            for (BluetoothDevice d : bonded) {
                Found f = describe(d, true, 0);
                devices.add(f);
                line("  " + f.line());
            }
        }
    }

    /**
     * Поиск в эфире. Классический Bluetooth: BLE-устройства сюда не попадут —
     * на API 10 их нечем увидеть, и это осознанное ограничение, а не пробел.
     */
    public void startDiscovery() {
        BluetoothAdapter a;
        try {
            a = BluetoothAdapter.getDefaultAdapter();
        } catch (Throwable t) {
            return;
        }
        if (a == null || adapterState != READY) {
            line("-- поиск не запускался: адаптера нет или он выключен --");
            return;
        }
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        try {
            context.registerReceiver(receiver, f);
        } catch (Throwable t) {
            line("registerReceiver: " + t);
            return;
        }
        try {
            if (a.isDiscovering()) {
                a.cancelDiscovery();
            }
            discoveryRan = a.startDiscovery();
            line("-- поиск в эфире (classic): " + (discoveryRan ? "запущен" : "ОТКЛОНЁН") + " --");
        } catch (Throwable t) {
            line("startDiscovery: " + t);
        }
    }

    public void stopDiscovery() {
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            if (a != null && a.isDiscovering()) {
                a.cancelDiscovery();
            }
        } catch (Throwable ignored) {
        }
        try {
            context.unregisterReceiver(receiver);
        } catch (Throwable ignored) {
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
            String action = intent == null ? null : intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice d =
                        (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                if (d == null) {
                    return;
                }
                Found f = describe(d, false, rssi);
                if (known(f.address)) {
                    return;
                }
                devices.add(f);
                line("  найдено: " + f.line());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                discoveryFinished = true;
                line("-- поиск завершён, устройств: " + devices.size() + " --");
            }
        }
    };

    private boolean known(String address) {
        for (int i = 0; i < devices.size(); i++) {
            String a = devices.get(i).address;
            if (a != null && a.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private static Found describe(BluetoothDevice d, boolean bonded, int rssi) {
        Found f = new Found();
        f.bonded = bonded;
        f.fromDiscovery = !bonded;
        f.rssi = rssi;
        try {
            f.name = d.getName();
        } catch (Throwable ignored) {
        }
        try {
            f.address = d.getAddress();
        } catch (Throwable ignored) {
        }
        try {
            f.bondState = d.getBondState();
        } catch (Throwable ignored) {
        }
        try {
            BluetoothClass bc = d.getBluetoothClass();
            if (bc != null) {
                f.deviceClass = bc.getDeviceClass();
                f.majorClass = bc.getMajorDeviceClass();
            }
        } catch (Throwable ignored) {
        }
        return f;
    }

    private static String bondName(int s) {
        if (s == BluetoothDevice.BOND_BONDED) {
            return "BONDED";
        }
        if (s == BluetoothDevice.BOND_BONDING) {
            return "BONDING";
        }
        return "NONE";
    }

    private void line(String s) {
        report.append(s).append('\n');
        Log.i(TAG, s);
    }
}
