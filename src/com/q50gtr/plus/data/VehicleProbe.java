package com.q50gtr.plus.data;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Диагностический зонд источников данных автомобиля.
 *
 * СТРОГО READ-ONLY: перечисляет, что доступно приложению на этом ГУ, и
 * ничего не настраивает, не отправляет и не меняет. Ни одного вызова в
 * сторону машины здесь нет.
 *
 * Зонд нужен до подключения «красивого» Dashboard: на незнакомой прошивке
 * важно сначала узнать факты, а не гадать. Отчёт уходит в logcat под тегом
 * Q50GTR/PROBE и доступен строкой для диагностического оверлея.
 */
public final class VehicleProbe {

    private static final String TAG = "Q50GTR/PROBE";
    private static final String CAN_PERMISSION = "com.ygomi.permission.IVI_CAN_READ";

    private final StringBuilder report = new StringBuilder();

    private int sensorCount;
    private int vehicleCount;
    private int mappedCount;
    private boolean permissionGranted;
    private String failure;

    public int getSensorCount() {
        return sensorCount;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public int getMappedCount() {
        return mappedCount;
    }

    public boolean isPermissionGranted() {
        return permissionGranted;
    }

    public String getFailure() {
        return failure;
    }

    public String getReport() {
        return report.toString();
    }

    /** Признак автомобильного сенсора: вендор, имя или диапазон типов. */
    public static boolean isVehicleSensor(Sensor s) {
        if (s == null) {
            return false;
        }
        String name = s.getName() == null ? "" : s.getName().toUpperCase();
        String vendor = s.getVendor() == null ? "" : s.getVendor().toUpperCase();
        int t = s.getType();
        return vendor.indexOf("YGOMI") >= 0
                || name.indexOf("VS_ID") >= 0
                || (t >= 12 && t <= 53);
    }

    public void run(Context ctx) {
        line("== Q50 GTR+ VEHICLE PROBE ==");
        line("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        line("device=" + Build.DEVICE + " model=" + Build.MODEL
                + " product=" + Build.PRODUCT);
        line("build=" + Build.DISPLAY);

        probePermission(ctx);
        probeSensors(ctx);

        line("== КОНЕЦ ОТЧЁТА ==");
    }

    private void probePermission(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            int r = pm.checkPermission(CAN_PERMISSION, ctx.getPackageName());
            permissionGranted = r == PackageManager.PERMISSION_GRANTED;
            line("permission " + CAN_PERMISSION + ": "
                    + (permissionGranted ? "GRANTED" : "DENIED (" + r + ")"));
        } catch (Throwable t) {
            line("permission check failed: " + t);
        }
    }

    private void probeSensors(Context ctx) {
        SensorManager sm;
        try {
            sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        } catch (Throwable t) {
            failure = "getSystemService(SENSOR_SERVICE): " + t;
            line(failure);
            return;
        }
        if (sm == null) {
            failure = "SENSOR_SERVICE недоступен";
            line(failure);
            return;
        }

        List<Sensor> all;
        try {
            all = sm.getSensorList(Sensor.TYPE_ALL);
        } catch (Throwable t) {
            failure = "getSensorList: " + t;
            line(failure);
            return;
        }
        if (all == null) {
            failure = "getSensorList вернул null";
            line(failure);
            return;
        }

        sensorCount = all.size();
        line("сенсоров всего: " + sensorCount);

        VehicleData probeData = new VehicleData();
        StringBuilder vehicle = new StringBuilder();
        StringBuilder other = new StringBuilder();
        StringBuilder unmapped = new StringBuilder();

        for (int i = 0; i < all.size(); i++) {
            Sensor s = all.get(i);
            String name = s.getName();
            if (isVehicleSensor(s)) {
                vehicleCount++;
                VehicleSignal sig = VehicleSignal.find(name);
                boolean mapped = sig != null
                        && VehicleSignal.channelFor(probeData, sig.vsId) != null;
                if (mapped) {
                    mappedCount++;
                }
                vehicle.append("  t").append(s.getType()).append(' ').append(name)
                        .append(mapped ? "  -> канал" : "  (не привязан)")
                        .append('\n');
                if (!mapped) {
                    unmapped.append("  ").append(name).append('\n');
                }
            } else {
                other.append("  t").append(s.getType()).append(' ').append(name)
                        .append(" (").append(s.getVendor()).append(")\n");
            }
        }

        line("автомобильных: " + vehicleCount + ", привязано к каналам: " + mappedCount);
        if (vehicleCount == 0) {
            failure = "автомобильных сенсоров нет — либо это не ГУ, либо нет "
                    + CAN_PERMISSION;
            line(failure);
        } else {
            line("-- автомобильные сенсоры --");
            report.append(vehicle);
        }
        if (unmapped.length() > 0) {
            // Это и есть список, ради которого зонд существует: имена, которые
            // ГУ отдаёт, а мы ещё не умеем принимать.
            line("-- есть на шине, но у нас нет привязки --");
            report.append(unmapped);
        }
        if (other.length() > 0) {
            line("-- прочие сенсоры --");
            report.append(other);
        }
    }

    private void line(String s) {
        report.append(s).append('\n');
        Log.i(TAG, s);
    }
}
