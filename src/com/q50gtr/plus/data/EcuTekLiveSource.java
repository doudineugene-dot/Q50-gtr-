package com.q50gtr.plus.data;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.q50gtr.plus.ecutek.EcuTekBluetoothProbe;
import com.q50gtr.plus.ecutek.EcuTekRawLog;

import java.io.InputStream;
import java.util.UUID;

/**
 * Связь с адаптером EcuTek EVI по Bluetooth.
 *
 * ЧТО ЗДЕСЬ ЕСТЬ: транспорт. Поиск адаптера, сопряжение (его состояние),
 * открытие RFCOMM-канала и приём байтов с записью их в сырой лог.
 *
 * ЧЕГО ЗДЕСЬ НЕТ И НЕ БУДЕТ, ПОКА НЕ ПОЯВЯТСЯ ДОКАЗАТЕЛЬСТВА: разбора кадров.
 * Прикладной протокол ECU Connect нигде не опубликован, открытых реализаций
 * нет. Поэтому ни одного байта не истолковано, ни один канал отсюда не
 * заполняется, и {@link #isConnected()} остаётся false даже при живом сокете:
 * «связь есть» и «данные есть» — разные вещи, и приборка не должна их путать.
 * Состояние транспорта видно отдельно, через {@link #getState()}.
 *
 * ИСТОЧНИК НИЧЕГО НЕ ПЕРЕДАЁТ. Сокет открывается только на чтение: в канал к
 * блоку управления двигателем не уходит ни одного байта. Отправлять наугад
 * кадры в ECU недопустимо, а осмысленных кадров у нас нет.
 *
 * Про исполнение адаптера: EcuTek делает EVI-BT на классическом Bluetooth и
 * EVI-BTLE на Bluetooth Low Energy. Публичного BLE API в Android до API 18 не
 * существует, а тут API 10, поэтому BTLE-исполнение этому ГУ недоступно и
 * даже не будет видно при поиске. Различать их заранее не нужно: если EVI
 * находится штатным поиском — он классический, и путь рабочий.
 */
public final class EcuTekLiveSource implements EcuTekSource {

    private static final String TAG = EcuTekBluetoothProbe.TAG;

    /* Состояния по ТЗ этапа. */
    public static final int DISABLED = 0;
    public static final int BLUETOOTH_OFF = 1;
    public static final int SEARCHING = 2;
    public static final int DEVICE_FOUND = 3;
    public static final int PAIRING = 4;
    public static final int CONNECTING = 5;
    public static final int CONNECTED = 6;
    public static final int SESSION_STARTING = 7;
    public static final int STREAMING = 8;
    public static final int STALE = 9;
    public static final int ERROR = 10;
    /** Адаптер EcuTek держит только одну сессию — её мог занять телефон. */
    public static final int BUSY = 11;

    private static final String[] STATE_NAME = {
            "DISABLED", "BLUETOOTH_OFF", "SEARCHING", "DEVICE_FOUND", "PAIRING",
            "CONNECTING", "CONNECTED", "SESSION_STARTING", "STREAMING", "STALE",
            "ERROR", "BUSY",
    };

    /**
     * Стандартный Serial Port Profile. Это не догадка о протоколе EcuTek, а
     * общий UUID для RFCOMM: на API 10 перечислить SDP-записи устройства
     * нечем — BluetoothDevice.getUuids() появился только в API 15. Если канал
     * у EVI другой, попытка честно провалится и это будет записано.
     */
    private static final UUID SPP =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /** Столько без байтов — поток считается замершим. */
    private static final long STALE_AFTER_MS = 1500L;

    private final Context context;
    private final EcuTekRawLog rawLog;

    private volatile int state = DISABLED;
    private volatile String deviceName;
    private volatile String deviceAddress;
    private volatile String lastError;
    private volatile long bytesIn;
    private volatile long packets;
    private volatile long lastRxMs;

    private EcuTekBluetoothProbe probe;
    private Thread worker;
    private volatile boolean running;
    private BluetoothSocket socket;

    public EcuTekLiveSource() {
        this(null, new EcuTekRawLog());
    }

    public EcuTekLiveSource(Context context, EcuTekRawLog rawLog) {
        this.context = context;
        this.rawLog = rawLog == null ? new EcuTekRawLog() : rawLog;
    }

    public String getName() {
        return "ECUTEK";
    }

    public int getState() {
        return state;
    }

    public String getStateName() {
        int s = state;
        return s >= 0 && s < STATE_NAME.length ? STATE_NAME[s] : "?";
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getLastError() {
        return lastError;
    }

    public long getBytesIn() {
        return bytesIn;
    }

    public long getPacketCount() {
        return packets;
    }

    public long getLastRxMs() {
        return lastRxMs;
    }

    public EcuTekRawLog getRawLog() {
        return rawLog;
    }

    public EcuTekBluetoothProbe getProbe() {
        return probe;
    }

    /* ------------------------------------------------------------------ */

    public void start() {
        if (context == null || running) {
            // Без Context Bluetooth не поднять — это обычный офлайн-режим
            // предпросмотра, а не ошибка.
            return;
        }
        running = true;
        worker = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "ecutek-bt");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
        closeSocket();
        if (probe != null) {
            probe.stopDiscovery();
        }
        Thread w = worker;
        if (w != null) {
            w.interrupt();
        }
        worker = null;
    }

    /**
     * Данные, а не сокет. Пока кадры не разобраны, источник не поставляет ни
     * одного канала, и признаваться живым ему не за что.
     */
    public boolean isConnected() {
        return false;
    }

    public void poll(VehicleData d, long nowMs) {
        if (state == STREAMING && nowMs - lastRxMs > STALE_AFTER_MS) {
            state = STALE;
        }
        // Разбора нет — писать в каналы нечего. Каналы EcuTek остаются
        // UNAVAILABLE, и на приборке в них стоит прочерк.
    }

    /* ------------------------------------------------------------------ */

    private void loop() {
        probe = new EcuTekBluetoothProbe(context);
        probe.probeAdapter();

        int adapter = probe.getAdapterState();
        if (adapter == EcuTekBluetoothProbe.NO_ADAPTER) {
            fail(DISABLED, "на этом ГУ нет Bluetooth-адаптера");
            return;
        }
        if (adapter == EcuTekBluetoothProbe.DISABLED) {
            fail(BLUETOOTH_OFF, "Bluetooth выключен");
            return;
        }

        EcuTekBluetoothProbe.Found evi = probe.findEvi();
        if (evi == null) {
            state = SEARCHING;
            probe.startDiscovery();
            // Классический поиск занимает около 12 секунд.
            for (int i = 0; i < 30 && running && !probe.isDiscoveryFinished(); i++) {
                sleep(500);
            }
            probe.stopDiscovery();
            evi = probe.findEvi();
        }

        if (evi == null) {
            fail(ERROR, "EVI не найден: ни среди сопряжённых, ни в эфире. "
                    + "Либо адаптер в исполнении BTLE (Android 2.3 его не видит), "
                    + "либо он вне зоны или спит");
            return;
        }

        deviceName = evi.name;
        deviceAddress = evi.address;
        state = DEVICE_FOUND;
        Log.i(TAG, "EVI найден: " + evi.line());

        if (!evi.bonded) {
            // createBond() появился только в API 15, здесь его нет. Сопрягать
            // нужно штатными настройками ГУ — и это не обход, а единственный
            // доступный путь на этой версии Android.
            fail(PAIRING, "EVI найден, но не сопряжён: " + evi.name + " ("
                    + evi.address + "). Сопрягите его в настройках Bluetooth ГУ");
            return;
        }

        connectAndListen(evi);
    }

    private void connectAndListen(EcuTekBluetoothProbe.Found evi) {
        state = CONNECTING;
        BluetoothDevice dev;
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            dev = a.getRemoteDevice(evi.address);
        } catch (Throwable t) {
            fail(ERROR, "getRemoteDevice: " + t);
            return;
        }

        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            if (a != null && a.isDiscovering()) {
                a.cancelDiscovery();   // поиск и соединение мешают друг другу
            }
        } catch (Throwable ignored) {
        }

        BluetoothSocket s;
        try {
            s = dev.createRfcommSocketToServiceRecord(SPP);
        } catch (Throwable t) {
            fail(ERROR, "createRfcommSocketToServiceRecord: " + t);
            return;
        }
        socket = s;
        rawLog.note("connect -> " + evi.address + " SPP " + SPP);

        try {
            s.connect();
        } catch (Throwable t) {
            String m = t.toString();
            String low = m.toLowerCase();
            if (low.indexOf("busy") >= 0 || low.indexOf("in use") >= 0
                    || low.indexOf("refused") >= 0) {
                // EcuTek допускает только одну сессию: вероятнее всего адаптер
                // занят телефоном с ECU Connect.
                fail(BUSY, "EVI занят другой сессией (ECU Connect на телефоне?): " + m);
            } else {
                fail(ERROR, "connect: " + m);
            }
            rawLog.note("connect FAILED: " + m);
            closeSocket();
            return;
        }

        state = CONNECTED;
        rawLog.note("connected");
        Log.i(TAG, "RFCOMM открыт: " + evi.name);

        InputStream in;
        try {
            in = s.getInputStream();
        } catch (Throwable t) {
            fail(ERROR, "getInputStream: " + t);
            closeSocket();
            return;
        }

        // Сессия не «стартуется»: отправить в ECU нечего, пока протокол не
        // разобран. Слушаем то, что адаптер шлёт сам.
        state = SESSION_STARTING;
        byte[] buf = new byte[512];
        while (running) {
            int n;
            try {
                n = in.read(buf);
            } catch (Throwable t) {
                rawLog.note("read failed: " + t);
                fail(ERROR, "обрыв чтения: " + t);
                break;
            }
            if (n < 0) {
                rawLog.note("поток закрыт удалённой стороной");
                fail(ERROR, "EVI закрыл поток");
                break;
            }
            if (n == 0) {
                continue;
            }
            bytesIn += n;
            packets++;
            lastRxMs = System.currentTimeMillis();
            state = STREAMING;
            rawLog.rx(buf, n);
        }
        closeSocket();
    }

    private void closeSocket() {
        BluetoothSocket s = socket;
        socket = null;
        if (s != null) {
            try {
                s.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private void fail(int newState, String reason) {
        state = newState;
        lastError = reason;
        Log.w(TAG, reason);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
