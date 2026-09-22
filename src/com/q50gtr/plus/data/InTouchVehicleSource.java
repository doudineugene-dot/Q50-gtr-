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

    /*
     * Сырой срез ВСЕХ автомобильных сенсоров: тип, имя, последнее значение.
     *
     * Имена в таблице сигналов подобраны по смыслу, а не сняты с этой
     * прошивки, и промах не отличить от «сигнала нет»: канал в обоих случаях
     * пустой. Обороты показывают 0 на заведённом моторе — без сырых значений
     * непонятно, врёт наша привязка или сам сигнал. Поэтому подписываемся на
     * все автомобильные сенсоры и держим последнее значение каждого: один
     * снимок экрана заменяет догадки.
     */
    private String[] dumpName = new String[0];
    private int[] dumpType = new int[0];
    private volatile float[] dumpValue = new float[0];
    private volatile boolean[] dumpSet = new boolean[0];

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

    /**
     * Сырой срез для диагностического оверлея: по строке на автомобильный
     * сенсор, «t13 ENGINE_RPM 0.0». Префикс VS_ID_ убран — он у всех
     * одинаковый и только съедает ширину.
     */
    public String[] getRawLines() {
        float[] dv = dumpValue;
        boolean[] ds = dumpSet;
        String[] out = new String[dumpName.length];
        for (int i = 0; i < out.length; i++) {
            String nm = dumpName[i] == null ? "?" : dumpName[i];
            if (nm.startsWith("VS_ID_")) {
                nm = nm.substring(6);
            }
            String v = (i < ds.length && ds[i]) ? fmt(dv[i]) : "--";
            out[i] = "t" + dumpType[i] + " " + nm + " = " + v;
        }
        return out;
    }

    private static String fmt(float v) {
        float a = v < 0f ? -v : v;
        if (a >= 100f) {
            return Integer.toString(Math.round(v));
        }
        return Channel.format(v, a >= 10f ? 1 : 3);
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

        // Сырой срез заводится до привязки: он нужен и тогда, когда не
        // совпало ни одно имя.
        int vehicles = 0;
        for (int i = 0; i < all.size(); i++) {
            if (VehicleProbe.isVehicleSensor(all.get(i))) {
                vehicles++;
            }
        }
        dumpName = new String[vehicles];
        dumpType = new int[vehicles];
        dumpValue = new float[vehicles];
        dumpSet = new boolean[vehicles];

        Sensor[] b = new Sensor[all.size()];
        VehicleSignal[] bs = new VehicleSignal[all.size()];
        int n = 0;
        int dn = 0;
        for (int i = 0; i < all.size(); i++) {
            Sensor s = all.get(i);
            VehicleSignal sig = VehicleSignal.find(s.getName());
            if (VehicleProbe.isVehicleSensor(s) && dn < vehicles) {
                dumpName[dn] = s.getName();
                dumpType[dn] = s.getType();
                dn++;
                if (sig == null) {
                    // Непривязанный сенсор нужен только для среза, поэтому
                    // самый медленный темп: лишняя нагрузка тут ни к чему.
                    try {
                        sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
                    } catch (Throwable ignored) {
                    }
                }
            }
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

        float[] dv = dumpValue;
        boolean[] ds = dumpSet;
        for (int i = 0; i < dumpName.length && i < dv.length; i++) {
            if (dumpName[i] != null && dumpName[i].equals(name)) {
                dv[i] = e.values[0];
                ds[i] = true;
                break;
            }
        }

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
        deriveRpm(d, nowMs);

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

    /**
     * Обороты, когда штатный сенсор их не отдаёт.
     *
     * Замер с этой машины на холостых в Park: ENGINE_RPM (t13) = 0.000 при
     * работающем моторе, EFFECTIVE_TORQUE (t12) = 9.000,
     * ENGINE_POWER (t32) = 5963. Отношение 5963 / 9 = 662.6 — ровно холостые
     * прогретого VR30DDTT. То есть t32 хранит произведение оборотов на
     * момент, и обороты из него восстанавливаются делением. Независимо к
     * тому же выводу пришёл открытый проект qazwsd147/appgarage-dash,
     * калибровавшийся на таком же VR30DDTT.
     *
     * Приоритет у прямого сенсора: если t13 отдал ненулевое значение, берём
     * его. Расчёт — только когда прямого нет. При нулевом или отрицательном
     * моменте (принудительный холостой ход, торможение двигателем) деление
     * бессмысленно, и тогда обороты просто не обновляются: лучше устаревшее
     * значение, чем выдуманное.
     */
    private void deriveRpm(VehicleData d, long nowMs) {
        if (d.rpm.hasValue() && d.rpm.getValue() > 0f) {
            return;
        }
        if (!d.enginePower.hasValue() || !d.engineTorque.hasValue()) {
            return;
        }
        float torque = d.engineTorque.getValue();
        float power = d.enginePower.getValue();
        if (torque < 1f || power <= 0f) {
            return;
        }
        float rpm = power / torque;
        if (rpm < 0f || rpm > 9000f) {
            return;   // за пределами шкалы — считаем замер негодным
        }
        d.rpm.setLive(rpm, NAME + "/CALC", nowMs);
    }
}
