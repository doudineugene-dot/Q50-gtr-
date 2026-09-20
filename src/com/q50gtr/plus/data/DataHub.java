package com.q50gtr.plus.data;

import android.util.Log;

/**
 * Владеет данными автомобиля и решает, кто их заполняет.
 *
 * Источники не конкурируют за все параметры разом: каждый пишет только свои
 * каналы. Штатный InTouch даёт обороты, скорость, температуры и давление
 * масла; EcuTek — детонацию, AFR, коррекции, наддув, HPFP; AirLift —
 * пневмоподвеску. См. docs/VEHICLE-DATA-ARCHITECTURE.md.
 *
 * Главное правило режима LIVE: отсутствующий реальный параметр НИКОГДА не
 * подменяется демо-значением. Как только хоть один живой источник дал кадр,
 * демо выключается насовсем до перезапуска, а каналы без данных остаются
 * UNAVAILABLE или уходят в STALE по таймауту.
 */
public final class DataHub {

    private static final String TAG = "Q50GTR/DATAHUB";

    private final VehicleData data = new VehicleData();

    private final DataSource inTouch;
    private final EcuTekSource ecuTek;
    private final AirLiftSource airLift;
    private final DemoDataProvider demo;

    private boolean demoAllowed = true;
    /**
     * Защёлка: однажды увидев живые данные, мы больше не возвращаемся к демо.
     * Иначе кратковременный обрыв связи подсунул бы на приборку выдуманные
     * числа ровно в тот момент, когда они опаснее всего.
     */
    private boolean everLive;

    public DataHub() {
        this(null, new EcuTekLiveSource(), new AirLiftLiveSource(), new DemoDataProvider());
    }

    public DataHub(DataSource inTouch, EcuTekSource ecuTek, AirLiftSource airLift,
                   DemoDataProvider demo) {
        this.inTouch = inTouch;
        this.ecuTek = ecuTek;
        this.airLift = airLift;
        this.demo = demo;
    }

    public VehicleData getData() {
        return data;
    }

    public void setDemoAllowed(boolean allowed) {
        demoAllowed = allowed;
    }

    public DataSource getInTouch() {
        return inTouch;
    }

    public EcuTekSource getEcuTek() {
        return ecuTek;
    }

    public AirLiftSource getAirLift() {
        return airLift;
    }

    public void start() {
        if (inTouch != null) {
            inTouch.start();
        }
        ecuTek.start();
        airLift.start();
        demo.start();
        Log.i(TAG, "старт: intouch=" + (inTouch != null)
                + " ecutek=" + ecuTek.getName() + " airlift=" + airLift.getName());
    }

    public void stop() {
        if (inTouch != null) {
            inTouch.stop();
        }
        ecuTek.stop();
        airLift.stop();
        demo.stop();
    }

    public void poll(long nowMs) {
        boolean live = false;

        if (inTouch != null && inTouch.isConnected()) {
            inTouch.poll(data, nowMs);
            live = true;
        }
        if (ecuTek.isConnected()) {
            ecuTek.poll(data, nowMs);
            live = true;
        }
        if (airLift.isConnected()) {
            airLift.poll(data, nowMs);
            live = true;
        }

        if (live && !everLive) {
            everLive = true;
            // Демо-значения, успевшие попасть в каналы на старте, снимаем:
            // иначе они остались бы на экране как «реальные».
            data.clearDemoValues();
            Log.i(TAG, "перешли в LIVE, демо-режим выключен до перезапуска");
        }

        if (!everLive && demoAllowed) {
            demo.poll(data, nowMs);
        }
    }

    /** Что показывать в строке источника. */
    public String getSourceLabel() {
        StringBuilder sb = new StringBuilder();
        if (inTouch != null && inTouch.isConnected()) {
            sb.append(inTouch.getName());
        }
        if (ecuTek.isConnected()) {
            append(sb, ecuTek.getName());
        }
        if (airLift.isConnected()) {
            append(sb, airLift.getName());
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        if (everLive) {
            return "NO LINK";
        }
        return demoAllowed ? demo.getName() : "NO LINK";
    }

    private static void append(StringBuilder sb, String name) {
        if (sb.length() > 0) {
            sb.append(" + ");
        }
        sb.append(name);
    }

    /** Демо активно только пока ни один живой источник ни разу не отвечал. */
    public boolean isDemoActive() {
        return !everLive && demoAllowed;
    }

    public boolean isLive() {
        return everLive;
    }
}
