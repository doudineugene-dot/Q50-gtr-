package com.q50gtr.plus.data;

/**
 * Привязка сигналов головного устройства к каналам {@link VehicleData}.
 *
 * ГУ Infiniti InTouch отдаёт шину не через SocketCAN, а как обычные
 * Android-сенсоры: вендор «Ygomi», имена VS_ID_*, типы 12..53. См.
 * docs/INTOUCH-VEHICLE-API-RESEARCH.md.
 *
 * Привязка идёт ПО ИМЕНИ, а не по номеру типа: номера — это порядок в списке
 * конкретной прошивки, имя — контракт вендора.
 *
 * Уверенность в каждой строке разная, и это записано явно. CONFIRMED — имя и
 * живое значение сняты с ГУ этой машины (docs/can-signals.md, типы 12..20).
 * CANDIDATE — имя правдоподобно, но на этой прошивке не наблюдалось; если
 * такого сенсора нет, строка просто не сработает, и канал останется
 * UNAVAILABLE. Выдумывать значение она не может по построению.
 */
public final class VehicleSignal {

    /** Имя сняли с живого ГУ вместе со значением. */
    public static final int CONFIRMED = 0;
    /** Имя предполагается; свяжется, только если сенсор реально есть. */
    public static final int CANDIDATE = 1;

    /** Как часто просить сенсор, мкс. Быстрые каналы — 20 Гц, температуры — 2 Гц. */
    public static final int RATE_FAST = 50000;
    public static final int RATE_MEDIUM = 100000;
    public static final int RATE_SLOW = 500000;

    public final String vsId;
    /** Множитель к сырому значению, приводящий его к единицам канала. */
    public final float scale;
    public final int confidence;
    public final int rateUs;
    /** Через сколько без обновления значение перестаёт считаться актуальным. */
    public final long staleAfterMs;

    private VehicleSignal(String vsId, float scale, int confidence,
                          int rateUs, long staleAfterMs) {
        this.vsId = vsId;
        this.scale = scale;
        this.confidence = confidence;
        this.rateUs = rateUs;
        this.staleAfterMs = staleAfterMs;
    }

    private static VehicleSignal fast(String id, float scale, int conf) {
        return new VehicleSignal(id, scale, conf, RATE_FAST, 1500L);
    }

    private static VehicleSignal medium(String id, float scale, int conf) {
        return new VehicleSignal(id, scale, conf, RATE_MEDIUM, 2000L);
    }

    private static VehicleSignal slow(String id, float scale, int conf) {
        return new VehicleSignal(id, scale, conf, RATE_SLOW, 10000L);
    }

    /**
     * Канал {@link VehicleData}, в который ложится сигнал.
     *
     * Не Map: на Android 2.3 это лишние аллокации на каждом кадре, а список
     * короткий и меняется только вместе с кодом.
     */
    public static Channel channelFor(VehicleData d, String vsId) {
        if ("VS_ID_ENGINE_RPM".equals(vsId)) {
            return d.rpm;
        }
        if ("VS_ID_VEHICLE_SPEED".equals(vsId)) {
            return d.speed;
        }
        if ("VS_ID_ENGINE_COOLANT_TEMPERATURE".equals(vsId)) {
            return d.coolantTemp;
        }
        if ("VS_ID_ENGINE_OIL_TEMPERATURE".equals(vsId)) {
            return d.oilTemp;
        }
        if ("VS_ID_ENGINE_OIL_PRESSURE".equals(vsId)) {
            return d.oilPressure;
        }
        if ("VS_ID_THROTTLE_POSITION".equals(vsId)) {
            return d.throttle;
        }
        if ("VS_ID_ACCELERATOR_PEDAL_POSITION".equals(vsId)) {
            return d.pedal;
        }
        if ("VS_ID_INTAKE_AIR_TEMPERATURE".equals(vsId)) {
            return d.intakeTemp;
        }
        if ("VS_ID_AMBIENT_TEMPERATURE".equals(vsId)
                || "VS_ID_OUTSIDE_TEMPERATURE".equals(vsId)) {
            return d.ambientTemp;
        }
        if ("VS_ID_BATTERY_VOLTAGE".equals(vsId)) {
            return d.batteryVoltage;
        }
        if ("VS_ID_TRANSMISSION_TEMPERATURE".equals(vsId)) {
            return d.transmissionTemp;
        }
        if ("VS_ID_BOOST_PRESSURE".equals(vsId) || "VS_ID_TURBO_BOOST".equals(vsId)) {
            return d.boostActual;
        }
        return null;
    }

    /** Таблица сигналов, которые мы умеем принимать. */
    public static final VehicleSignal[] TABLE = {
            // --- снято с живого ГУ этой машины ---
            fast("VS_ID_ENGINE_RPM", 1f, CONFIRMED),
            medium("VS_ID_VEHICLE_SPEED", 1f, CONFIRMED),
            slow("VS_ID_ENGINE_COOLANT_TEMPERATURE", 1f, CONFIRMED),
            slow("VS_ID_ENGINE_OIL_TEMPERATURE", 1f, CONFIRMED),
            // Сырое значение в МПа: 0.163 на заглушённом моторе. x10 -> бар.
            // Множитель взят из открытого qazwsd147/appgarage-dash и требует
            // проверки на заведённом моторе.
            medium("VS_ID_ENGINE_OIL_PRESSURE", 10f, CONFIRMED),

            // --- имена предположительные, свяжутся только если сенсор есть ---
            fast("VS_ID_THROTTLE_POSITION", 1f, CANDIDATE),
            fast("VS_ID_ACCELERATOR_PEDAL_POSITION", 1f, CANDIDATE),
            fast("VS_ID_BOOST_PRESSURE", 10f, CANDIDATE),
            fast("VS_ID_TURBO_BOOST", 10f, CANDIDATE),
            slow("VS_ID_INTAKE_AIR_TEMPERATURE", 1f, CANDIDATE),
            slow("VS_ID_AMBIENT_TEMPERATURE", 1f, CANDIDATE),
            slow("VS_ID_OUTSIDE_TEMPERATURE", 1f, CANDIDATE),
            slow("VS_ID_BATTERY_VOLTAGE", 1f, CANDIDATE),
            slow("VS_ID_TRANSMISSION_TEMPERATURE", 1f, CANDIDATE),
    };

    /** Строка таблицы по имени сигнала, или null. */
    public static VehicleSignal find(String vsId) {
        if (vsId == null) {
            return null;
        }
        String up = vsId.toUpperCase();
        for (int i = 0; i < TABLE.length; i++) {
            if (TABLE[i].vsId.equals(up)) {
                return TABLE[i];
            }
        }
        return null;
    }

}
