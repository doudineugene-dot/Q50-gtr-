package com.q50gtr.plus.data;

/**
 * Every channel the dashboard can draw, in one place.
 *
 * The field list is deliberately shaped after what the car actually logs
 * through EcuTek on the VR30DDTT, so that swapping DemoDataProvider for a real
 * EcuTek source later is a data-layer change only — no gauge is rewritten.
 *
 * Turbo speed is absent on purpose: it is not logged on this vehicle.
 */
public final class VehicleData {

    /* --- engine ------------------------------------------------------- */
    public final Channel rpm = new Channel("rpm", "RPM", "");
    /*
     * Момент и мощность нужны не для показа, а для восстановления оборотов.
     * На этой машине сенсор ENGINE_RPM (t13) на стоянке отдаёт ноль при
     * работающем моторе, а ENGINE_POWER (t32) при этом равен произведению
     * оборотов на момент: 5963 = 663 x 9 при холостых и 9 Нм. См.
     * docs/INTOUCH-VEHICLE-API-RESEARCH.md.
     */
    public final Channel engineTorque = new Channel("torque", "МОМЕНТ", "Nm");
    public final Channel enginePower = new Channel("power_raw", "МОЩН. СЫРАЯ", "");
    /** Селектор передач как число: P=1, R=2, N=3, D=4, M1..M7=16..22. */
    public final Channel gearPosition = new Channel("gear", "ПЕРЕДАЧА", "");
    public final Channel boostActual = new Channel("boost_actual", "BOOST ACT", "bar");
    public final Channel boostTarget = new Channel("boost_target", "BOOST TGT", "bar");
    public final Channel coolantTemp = new Channel("coolant_temp", "ОХЛ. ЖИДКОСТЬ", "°C");
    public final Channel oilTemp = new Channel("oil_temp", "МАСЛО T", "°C");
    public final Channel oilPressure = new Channel("oil_pressure", "МАСЛО P", "bar");
    public final Channel intakeTemp = new Channel("intake_temp", "ВПУСК T", "°C");

    /* --- fuel --------------------------------------------------------- */
    public final Channel hpfpActual = new Channel("hpfp_actual", "HPFP ACT", "bar");
    public final Channel hpfpTarget = new Channel("hpfp_target", "HPFP TGT", "bar");
    public final Channel afrB1 = new Channel("afr_b1", "AFR B1", "");
    public final Channel afrB2 = new Channel("afr_b2", "AFR B2", "");
    public final Channel stftB1 = new Channel("stft_b1", "STFT B1", "%");
    public final Channel stftB2 = new Channel("stft_b2", "STFT B2", "%");
    public final Channel ltftB1 = new Channel("ltft_b1", "LTFT B1", "%");
    public final Channel ltftB2 = new Channel("ltft_b2", "LTFT B2", "%");

    /**
     * Low pressure fuel pump. NOT logged in the current EcuTek configuration,
     * so it is permanently UNAVAILABLE and the fuel page renders it as such.
     * It exists as a field only so a future configuration can fill it in.
     */
    public final Channel lpfp = new Channel("lpfp", "LPFP", "bar");

    /* --- ignition / knock --------------------------------------------- */
    public final Channel ignitionTiming = new Channel("ign_timing", "IGN TIMING", "°");
    public final Channel knockRetard = new Channel("knock_retard", "KNOCK RETARD", "°");
    /** Knock index, cylinders 1..6 (index 0 == cylinder 1). */
    public final Channel[] knockIndex = new Channel[6];

    /* --- driver inputs / vehicle -------------------------------------- */
    public final Channel throttle = new Channel("throttle", "THROTTLE", "%");
    public final Channel pedal = new Channel("pedal", "PEDAL", "%");
    public final Channel speed = new Channel("speed", "SPEED", "km/h");

    /* --- cam / valve timing ------------------------------------------- */
    public final Channel ivtIntakeB1 = new Channel("ivt_in_b1", "IVT IN B1", "°");
    public final Channel ivtExhaustB1 = new Channel("ivt_ex_b1", "IVT EX B1", "°");
    public final Channel ivtIntakeB2 = new Channel("ivt_in_b2", "IVT IN B2", "°");
    public final Channel ivtExhaustB2 = new Channel("ivt_ex_b2", "IVT EX B2", "°");

    /* --- chassis ------------------------------------------------------ */
    public final Channel transmissionTemp = new Channel("trans_temp", "АКПП", "°C");
    public final Channel transferCaseTemp = new Channel("transfer_temp", "РАЗДАТКА", "°C");
    public final Channel batteryVoltage = new Channel("voltage", "БОРТ. СЕТЬ", "V");
    public final Channel ambientTemp = new Channel("ambient_temp", "СНАРУЖИ", "°C");

    /* --- AirLift Performance 3H (suspension) --------------------------- */
    public final Channel airFrontLeft = new Channel("air_fl", "FL", "bar");
    public final Channel airFrontRight = new Channel("air_fr", "FR", "bar");
    public final Channel airRearLeft = new Channel("air_rl", "RL", "bar");
    public final Channel airRearRight = new Channel("air_rr", "RR", "bar");
    public final Channel airTank = new Channel("air_tank", "РЕСИВЕР", "bar");

    public VehicleData() {
        for (int i = 0; i < knockIndex.length; i++) {
            knockIndex[i] = new Channel("knock_idx_" + (i + 1), "CYL " + (i + 1), "");
        }
        // LPFP is not logged; say so once and never touch it again.
        lpfp.setUnavailable();
    }

    /** Highest knock index across cylinders 1..6, or null when none is known. */
    /**
     * Селектор передач словом. Перечисление снято на автомобиле проектом
     * qazwsd147/appgarage-dash и сходится с наблюдением: на стоянке в Park
     * сенсор отдавал ровно 1.
     */
    public String gearText() {
        if (!gearPosition.hasValue()) {
            return "—";
        }
        int g = (int) (gearPosition.getValue() + 0.5f);
        if (g == 1) {
            return "P";
        }
        if (g == 2) {
            return "R";
        }
        if (g == 3) {
            return "N";
        }
        if (g == 4) {
            return "D";
        }
        if (g >= 16 && g <= 22) {
            return "M" + (g - 15);
        }
        return "—";
    }

    public Channel maxKnockIndexChannel() {
        Channel best = null;
        for (int i = 0; i < knockIndex.length; i++) {
            Channel c = knockIndex[i];
            if (!c.hasValue()) {
                continue;
            }
            if (best == null || c.getValue() > best.getValue()) {
                best = c;
            }
        }
        return best;
    }

    /** True while any drawn channel is still synthetic. */
    public boolean anyDemo() {
        return rpm.isDemo() || boostActual.isDemo() || airFrontLeft.isDemo();
    }

    /**
     * Снимает все демо-значения при переходе в LIVE.
     *
     * Без этого числа, нарисованные демо-провайдером до появления связи,
     * остались бы на приборке и выглядели бы как показания машины — ровно то,
     * чего допускать нельзя.
     */
    public void clearDemoValues() {
        Channel[] all = allChannels();
        for (int i = 0; i < all.length; i++) {
            if (all[i].isDemo()) {
                all[i].setUnavailable();
            }
        }
    }

    /** Все каналы одним списком: для сброса демо и для диагностики. */
    public Channel[] allChannels() {
        Channel[] fixed = {
                rpm, boostActual, boostTarget, coolantTemp, oilTemp, oilPressure,
                intakeTemp, hpfpActual, hpfpTarget, afrB1, afrB2,
                stftB1, stftB2, ltftB1, ltftB2, lpfp,
                ignitionTiming, knockRetard, throttle, pedal, speed,
                ivtIntakeB1, ivtExhaustB1, ivtIntakeB2, ivtExhaustB2,
                transmissionTemp, transferCaseTemp, batteryVoltage, ambientTemp,
                airFrontLeft, airFrontRight, airRearLeft, airRearRight, airTank,
        };
        Channel[] out = new Channel[fixed.length + knockIndex.length];
        System.arraycopy(fixed, 0, out, 0, fixed.length);
        System.arraycopy(knockIndex, 0, out, fixed.length, knockIndex.length);
        return out;
    }
}
