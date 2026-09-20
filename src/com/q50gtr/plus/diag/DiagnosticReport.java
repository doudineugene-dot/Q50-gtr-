package com.q50gtr.plus.diag;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Сохранение диагностического отчёта на флешку.
 *
 * Тест идёт в машине, и adb там может не быть. Поэтому отчёт пишется файлом на
 * тот же накопитель, с которого ставился пакет: вынул флешку — и он у тебя.
 *
 * Секреты сюда не попадают: отчёт состоит из геометрии окна, списка сенсоров и
 * состояний каналов.
 */
public final class DiagnosticReport {

    public static final String FILE_NAME = "Q50GTR-diagnostic.txt";
    private static final String TAG = "Q50GTR/REPORT";

    private DiagnosticReport() {
    }

    /** Возвращает путь записанного файла или null, если писать некуда. */
    public static String write(String body) {
        File dir = UsbStorage.pickWritableDir();
        if (dir == null) {
            Log.w(TAG, "не нашёл, куда писать отчёт");
            return null;
        }
        File out = new File(dir, FILE_NAME);
        Writer w = null;
        try {
            w = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
            w.write(body);
            w.flush();
            Log.i(TAG, "отчёт записан: " + out.getAbsolutePath());
            return out.getAbsolutePath();
        } catch (Throwable t) {
            Log.w(TAG, "не удалось записать " + out.getAbsolutePath() + ": " + t);
            return null;
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
