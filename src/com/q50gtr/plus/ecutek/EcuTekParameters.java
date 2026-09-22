package com.q50gtr.plus.ecutek;

import com.q50gtr.plus.data.VehicleData;

/**
 * Каталог параметров EcuTek: что нам нужно от EVI и что из этого известно.
 *
 * Пока сессия с адаптером не разобрана, каталог заполняется не «оттуда», а
 * отсюда: это список запрошенных каналов со статусом UNKNOWN. Как только EVI
 * начнёт отдавать собственный перечень, сюда лягут его raw ID, имена, единицы
 * и масштабы, и статус сменится на SUPPORTED/UNSUPPORTED.
 *
 * Выдуманных ID, масштабов и порядков байт здесь нет и быть не должно: файл
 * существует ровно для того, чтобы отличать известное от неизвестного.
 */
public final class EcuTekParameters {

    public static final String FILE_NAME = "ecutek-parameters.txt";

    /** Каналы, ради которых EcuTek вообще нужен: штатный CAN их не отдаёт. */
    private static final String[][] WANTED = {
            // имя канала           единица   заметка
            {"RPM",                 "rpm",    "первый рубеж: доказать один канал"},
            {"BOOST_ACTUAL",        "bar",    "штатного CAN не существует"},
            {"BOOST_TARGET",        "bar",    "только EcuTek"},
            {"AFR_B1",              "afr",    "только EcuTek"},
            {"AFR_B2",              "afr",    "только EcuTek"},
            {"IGNITION_TIMING",     "deg",    "только EcuTek"},
            {"KNOCK_INDEX_CYL1",    "-",      "хранить по цилиндрам, не усреднять"},
            {"KNOCK_INDEX_CYL2",    "-",      "хранить по цилиндрам, не усреднять"},
            {"KNOCK_INDEX_CYL3",    "-",      "хранить по цилиндрам, не усреднять"},
            {"KNOCK_INDEX_CYL4",    "-",      "хранить по цилиндрам, не усреднять"},
            {"KNOCK_INDEX_CYL5",    "-",      "хранить по цилиндрам, не усреднять"},
            {"KNOCK_INDEX_CYL6",    "-",      "хранить по цилиндрам, не усреднять"},
            {"KNOCK_RETARD",        "deg",    "только EcuTek"},
            {"STFT_B1",             "%",      "хранить отдельно от B2"},
            {"STFT_B2",             "%",      "хранить отдельно от B1"},
            {"LTFT_B1",             "%",      "хранить отдельно от B2"},
            {"LTFT_B2",             "%",      "хранить отдельно от B1"},
            {"HPFP_ACTUAL",         "bar",    "только EcuTek"},
            {"HPFP_TARGET",         "bar",    "подключать, только если реально есть"},
            {"THROTTLE",            "%",      "есть и в штатном CAN — нужен приоритет"},
            {"PEDAL",               "%",      "есть и в штатном CAN"},
            {"SPEED",               "km/h",   "есть и в штатном CAN"},
            {"COOLANT_TEMP",        "C",      "есть и в штатном CAN"},
    };

    private EcuTekParameters() {
    }

    public static String build(VehicleData d, EcuTekBluetoothProbe probe,
                               String sessionState, String sessionError) {
        StringBuilder b = new StringBuilder();
        b.append("== Q50 GTR+ : КАТАЛОГ ПАРАМЕТРОВ ECUTEK ==\n\n");
        b.append("состояние сессии: ").append(sessionState).append('\n');
        if (sessionError != null) {
            b.append("последняя ошибка: ").append(sessionError).append('\n');
        }
        b.append('\n');
        b.append("Каталог от адаптера НЕ получен: прикладной протокол ECU Connect\n");
        b.append("нигде не опубликован, открытых реализаций нет, а выдумывать ID и\n");
        b.append("масштабы запрещено. Ниже — запрошенная сторона: какие каналы нам\n");
        b.append("нужны от EVI. Колонки raw ID / scale / offset останутся пустыми,\n");
        b.append("пока их не даст сам адаптер.\n\n");

        b.append(pad("EcuTek name", 20)).append(pad("unit", 7))
                .append(pad("raw ID", 9)).append(pad("type", 7))
                .append(pad("scale", 7)).append(pad("offset", 7))
                .append(pad("min/max", 9)).append("status\n");
        b.append("-----------------------------------------------------------------------------\n");
        for (int i = 0; i < WANTED.length; i++) {
            b.append(pad(WANTED[i][0], 20)).append(pad(WANTED[i][1], 7))
                    .append(pad("-", 9)).append(pad("-", 7))
                    .append(pad("-", 7)).append(pad("-", 7))
                    .append(pad("-", 9)).append("UNKNOWN   ").append(WANTED[i][2]).append('\n');
        }

        b.append('\n');
        b.append("-- явно исключённое --\n");
        b.append("LPFP        : UNAVAILABLE. Прямого канала давления в нашей\n");
        b.append("              конфигурации EcuTek не подтверждено. Угол/цель\n");
        b.append("              топливного насоса в бары НЕ пересчитывается.\n");
        b.append("TURBO_SPEED : НЕ ИСПОЛЬЗУЕТСЯ. Параметр не логируется.\n");
        return b.toString();
    }

    private static String pad(String s, int n) {
        StringBuilder b = new StringBuilder(s == null ? "-" : s);
        while (b.length() < n) {
            b.append(' ');
        }
        return b.toString();
    }
}
