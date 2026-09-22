package com.q50gtr.plus.ecutek;

import android.util.Log;

/**
 * Запись сырого обмена с EVI: время, направление, длина, байты в HEX.
 *
 * По умолчанию ВЫКЛЮЧЕНА. Включается наличием файла-маркера в корне флешки
 * (см. {@link #MARKER}) — на этом ГУ нет ни adb, ни настроек, а флешку
 * пользователь и так вставляет для установки, так что маркер — самый простой
 * явный переключатель, который нельзя задеть случайно.
 *
 * Лог нужен ровно для одного: если первый разбор кадров окажется неполным,
 * по сырым байтам его можно доделать, не гоняя человека в машину заново.
 */
public final class EcuTekRawLog {

    /** Положите пустой файл с таким именем в корень флешки, чтобы включить. */
    public static final String MARKER = "Q50GTR-RAW";
    public static final String FILE_NAME = "ecutek-bluetooth-raw.log";

    private static final String TAG = "Q50GTR/ECUTEK_RAW";
    /** Потолок, чтобы лог не съел флешку на долгой поездке. */
    private static final int MAX_LINES = 20000;

    private final StringBuilder out = new StringBuilder();
    private boolean enabled;
    private int lines;
    private boolean truncated;
    private long t0;

    public void setEnabled(boolean on) {
        enabled = on;
        t0 = System.currentTimeMillis();
        Log.i(TAG, "сырой лог " + (on ? "ВКЛЮЧЁН (найден " + MARKER + ")" : "выключен"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getLineCount() {
        return lines;
    }

    public void rx(byte[] buf, int len) {
        append("RX", buf, len);
    }

    public void tx(byte[] buf, int len) {
        append("TX", buf, len);
    }

    /** Событие транспорта в том же файле: связь рвётся между кадрами. */
    public synchronized void note(String text) {
        if (!enabled || truncated) {
            return;
        }
        out.append(stamp()).append(" -- ").append(text).append('\n');
        count();
    }

    private synchronized void append(String dir, byte[] buf, int len) {
        if (!enabled || truncated || len <= 0) {
            return;
        }
        out.append(stamp()).append(' ').append(dir).append(' ').append(len).append("  ");
        for (int i = 0; i < len; i++) {
            int v = buf[i] & 0xFF;
            if (v < 0x10) {
                out.append('0');
            }
            out.append(Integer.toHexString(v));
            out.append(' ');
        }
        out.append('\n');
        count();
    }

    private void count() {
        lines++;
        if (lines >= MAX_LINES) {
            truncated = true;
            out.append("-- предел ").append(MAX_LINES).append(" строк, запись остановлена --\n");
        }
    }

    private String stamp() {
        long ms = System.currentTimeMillis() - t0;
        return "+" + ms + "ms";
    }

    public synchronized String dump() {
        if (!enabled) {
            return "Сырой лог выключен.\n"
                    + "Чтобы включить: положите пустой файл " + MARKER
                    + " в корень флешки и запустите приложение.\n";
        }
        if (lines == 0) {
            return "Сырой лог включён, но с EVI не пришло ни одного байта.\n";
        }
        return out.toString();
    }
}
