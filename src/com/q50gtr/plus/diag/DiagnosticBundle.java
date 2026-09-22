package com.q50gtr.plus.diag;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Один файл, который пользователь привозит из машины.
 *
 * В машине нет ни adb, ни возможности разбираться, какой из отчётов нужен,
 * поэтому всё складывается в единственный архив в корне флешки.
 */
public final class DiagnosticBundle {

    public static final String FILE_NAME = "Q50GTR-ECUTEK-DIAG.zip";
    private static final String TAG = "Q50GTR/BUNDLE";

    private DiagnosticBundle() {
    }

    /** Возвращает путь архива или null, если писать некуда. */
    public static String write(String bluetoothInfo, String parameters,
                               String rawLog, String diagnostic) {
        File dir = UsbStorage.pickWritableDir();
        if (dir == null) {
            Log.w(TAG, "не нашёл, куда писать архив");
            return null;
        }
        File out = new File(dir, FILE_NAME);
        ZipOutputStream z = null;
        try {
            z = new ZipOutputStream(new FileOutputStream(out));
            put(z, "bluetooth-info.txt", bluetoothInfo);
            put(z, com.q50gtr.plus.ecutek.EcuTekParameters.FILE_NAME, parameters);
            put(z, com.q50gtr.plus.ecutek.EcuTekRawLog.FILE_NAME, rawLog);
            put(z, "q50gtr-diagnostic.txt", diagnostic);
            z.finish();
            Log.i(TAG, "архив записан: " + out.getAbsolutePath());
            return out.getAbsolutePath();
        } catch (Throwable t) {
            Log.w(TAG, "не удалось записать " + out.getAbsolutePath() + ": " + t);
            return null;
        } finally {
            if (z != null) {
                try {
                    z.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void put(ZipOutputStream z, String name, String body) throws Exception {
        if (body == null) {
            body = "(нет данных)\n";
        }
        z.putNextEntry(new ZipEntry(name));
        z.write(body.getBytes("UTF-8"));
        z.closeEntry();
    }
}
