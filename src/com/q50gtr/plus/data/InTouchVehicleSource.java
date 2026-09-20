package com.q50gtr.plus.data;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.List;

/**
 * Штатный источник данных головного устройства Infiniti InTouch (DCU Gen1).
 *
 * ГУ не даёт Android-стороне ни SocketCAN, ни /dev/can — это проверено на
 * живом устройстве. Зато шина выведена как обычные Android-сенсоры: вендор
 * «Ygomi», имена VS_ID_*, типы 12..53, разрешение
 * com.ygomi.permission.IVI_CAN_READ. Подробности и происхождение каждого
 * факта — docs/INTOUCH-VEHICLE-API-RESEARCH.md.
 *
 * Источник только читает. Ни одного вызова, который что-либо отправляет в
 * машину, здесь нет и быть не должно.
 */
public final class InTouchVehicleSource implements DataSource, SensorEventListener {

    public static final String NAME = "INTOUCH";
    private static final String TAG = "Q50GTR/CAN";

    private final Context context;
    private SensorManager sm;

    /** Подписанные сенсоры и строка таблицы для каждого, по индексу. */
    private Sensor[] bound = new Sensor[0];
    private VehicleSignal[] boundSignal = new VehicleSignal[0];

    private volatile boolean connected;
    private volatile int frames;
    private volatile long lastFrameMs;
    private String lastError;

    /**
     * Сырые значения складываются сюда в потоке сенсоров, а в VehicleData
     * переносятся на тике UI. Так UI-поток не ловит гонку с колбэком.
     */
    private final float[] pending = new float[VehicleSignal.TABLE.length];
    private final long[] pendingAt = new long[VehicleSignal.TABLE.length];
    private final boolean[] pendingSet = new boolean[VehicleSignal.TABLE.length];

    public InTouchVehicleSource(Context context) {
        this.context = context;
    }

    public String getName() {
        return NAME;
    }

    public boolean isConnected() {
        return connected;
    }

    public int getFrameCount() {
        return frames;
    }

    public long getLastFrameMs() {
        return lastFrameMs;
    }

    public String getLastError() {
        return lastError;
    }

    public int getBoundCount() {
        return bound.length;
    }

    /* ------------------------------------------------------------------ */

    public void start() {
        if (sm != null) {
            return;
        }
        try {
            sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        } catch (Throwable t) {
            fail("getSystemService(SENSOR_SERVICE): " + t);
            return;
        }
        if (sm == null) {
            fail("SENSOR_SERVICE недоступен");
            return;
        }

        List<Sensor> all;
        try {
            all = sm.getSensorList(Sensor.TYPE_ALL);
        } catch (Throwable t) {
            fail("getSensorList: " + t);
            return;
        }
        if (all == null || all.isEmpty()) {
            fail("список сенсоров пуст");
            return;
        }

        Sensor[] b = new Sensor[all.size()];
        VehicleSignal[] bs = new VehicleSignal[all.size()];
        int n = 0;
        for (int i = 0; i < all.size(); i++) {
            Sensor s = all.get(i);
            VehicleSignal sig = VehicleSignal.find(s.getName());
            if (sig == null) {
                continue;
            }
            boolean ok;
            try {
                ok = sm.registerListener(this, s, sig.rateUs);
            } catch (SecurityException se) {
                // Нет IVI_CAN_READ — это отдельная, вполне конкретная причина.
                fail("SecurityException на " + s.getName()
                        + ": нет com.ygomi.permission.IVI_CAN_READ");
                continue;
            } catch (Throwable t) {
                Log.w(TAG, "registerListener " + s.getName() + ": " + t);
                continue;
            }
            if (!ok) {
                Log.w(TAG, "registerListener отклонён: " + s.getName());
                continue;
            }
            b[n] = s;
            bs[n] = sig;
            n++;
            Log.i(TAG, "подписан t" + s.getType() + " " + s.getName()
                    + " rate=" + sig.rateUs + "us"
                    + (sig.confidence == VehicleSignal.CONFIRMED ? " CONFIRMED" : " CANDIDATE"));
        }

        bound = new Sensor[n];
        boundSignal = new VehicleSignal[n];
        System.arraycopy(b, 0, bound, 0, n);
        System.arraycopy(bs, 0, boundSignal, 0, n);

        Log.i(TAG, "сенсоров всего " + all.size() + ", привязано сигналов " + n);
        if (n == 0) {
            fail("ни один VS_ID_* не совпал с таблицей");
        }
    }

    public void stop() {
        if (sm != null) {
            try {
                sm.unregisterListener(this);
            } catch (Throwable ignored) {
            }
        }
        sm = null;
        connected = false;
        bound = new Sensor[0];
        boundSignal = new VehicleSignal[0];
    }

    private void fail(String reason) {
        lastError = reason;
        connected = false;
        Log.w(TAG, reason);
    }

    /* --- поток сенсоров --- */

    public void onSensorChanged(SensorEvent e) {
        if (e == null || e.values == null || e.values.length == 0) {
            return;
        }
        String name = e.sensor == null ? null : e.sensor.getName();
        for (int i = 0; i < VehicleSignal.TABLE.length; i++) {
            if (VehicleSignal.TABLE[i].vsId.equalsIgnoreCase(name)) {
                pending[i] = e.values[0];
                pendingAt[i] = System.currentTimeMillis();
                pendingSet[i] = true;
                frames++;
                lastFrameMs = pendingAt[i];
                // Связь считается живой с первого же кадра из шины.
                connected = true;
                return;
            }
        }
    }

    public void onAccuracyChanged(Sensor s, int accuracy) {
    }

    /* --- тик UI --- */

    public void poll(VehicleData d, long nowMs) {
        for (int i = 0; i < VehicleSignal.TABLE.length; i++) {
            if (!pendingSet[i]) {
                continue;
            }
            VehicleSignal sig = VehicleSignal.TABLE[i];
            Channel c = VehicleSignal.channelFor(d, sig.vsId);
            if (c != null) {
                c.setLive(pending[i] * sig.scale, NAME, pendingAt[i]);
            }
            pendingSet[i] = false;
        }
        // Значение, которое перестало приходить, не должно и дальше выглядеть
        // актуальным: таймаут свой у каждого сигнала.
        for (int i = 0; i < boundSignal.length; i++) {
            VehicleSignal sig = boundSignal[i];
            Channel c = VehicleSignal.channelFor(d, sig.vsId);
            if (c != null) {
                c.ageOut(nowMs, sig.staleAfterMs);
            }
        }
    }
}
