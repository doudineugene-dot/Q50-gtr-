package com.q50gtr.plus.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Сборка кадров трансляции экрана ALP3 из кусков UDP.
 *
 * Формат описан в {@code docs/ALP-MIRROR-PROTOCOL.md}. Здесь — только
 * приём и сборка: ни один байт отсюда не идёт в {@link
 * com.q50gtr.plus.data.DataHub}, потому что это картинка, а не измеренный
 * канал. Числа с неё не читаются и не должны читаться — так требует
 * четвёртый этап задачи: источник данных Air Lift и трансляция экрана его
 * приложения остаются двумя разными возможностями, пока протокол манифолда
 * не подтверждён отдельно.
 *
 * Разбор пакета не бросает исключений наружу: битый или укороченный кусок
 * тихо увеличивает счётчик ошибок и не портит уже собранный кадр.
 */
public final class AlpFrameAssembler {

    private static final String TAG = "Q50GTR/ALP";

    /** Первые четыре байта пакета кадра — этим приёмник моста отличает его
     *  от текстовой телеметрии до попытки разобрать текст. */
    public static final byte[] MAGIC = {0x51, 0x31, 0x46, 0x52}; // "Q1FR"

    private static final int HEADER_LEN = 26;
    private static final long FRAME_TIMEOUT_MS = 2000L;

    private final Object lock = new Object();

    private int pendingFrameId = -1;
    private int pendingTotal;
    private byte[][] pendingChunks;
    private int pendingReceived;
    private int pendingWidth;
    private int pendingHeight;
    private long pendingTsMs;
    private long pendingStartedAtMs;

    private volatile Bitmap latestBitmap;
    private volatile int latestWidth;
    private volatile int latestHeight;
    private volatile long latestCompletedAtMs;
    private volatile long latestSourceTsMs;
    private volatile long latestFrameId = -1L;

    private volatile long framesCompleted;
    private volatile long framesDropped;
    private volatile long framesBroken;
    private volatile long chunksReceived;

    /* Скользящее окно для fps: время завершения последних кадров. */
    private final long[] recentMs = new long[16];
    private int recentAt;

    /** Похож ли пакет на кадр трансляции — проверяется до разбора текста. */
    public static boolean looksLikeFrame(byte[] buf, int len) {
        if (len < HEADER_LEN) {
            return false;
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (buf[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /** Разбирает один пакет-кусок кадра. Вызывается из потока приёма моста. */
    public void offer(byte[] buf, int len) {
        try {
            offerChecked(buf, len);
        } catch (Throwable t) {
            framesBroken++;
            Log.w(TAG, "кусок кадра не разобран: " + t);
        }
    }

    private void offerChecked(byte[] buf, int len) {
        if (len < HEADER_LEN) {
            framesBroken++;
            return;
        }
        int frameId = i32(buf, 4);
        int totalChunks = u16(buf, 8);
        int chunkIndex = u16(buf, 10);
        int width = u16(buf, 12);
        int height = u16(buf, 14);
        long tsMs = i64(buf, 16);
        int payloadLen = u16(buf, 24);

        if (totalChunks <= 0 || totalChunks > 512
                || chunkIndex < 0 || chunkIndex >= totalChunks
                || payloadLen < 0 || HEADER_LEN + payloadLen > len) {
            framesBroken++;
            return;
        }

        synchronized (lock) {
            long now = System.currentTimeMillis();

            if (pendingFrameId >= 0 && now - pendingStartedAtMs > FRAME_TIMEOUT_MS) {
                // Кадр завис: недособрался вовремя. Считаем потерянным, а не
                // показываем половину.
                framesDropped++;
                pendingFrameId = -1;
            }

            if (frameId < pendingFrameId) {
                // Трансляция не отматывает назад: более старый кадр, чем уже
                // собираемый, не имеет смысла собирать.
                return;
            }
            if (frameId > pendingFrameId) {
                if (pendingFrameId >= 0 && pendingReceived < pendingTotal) {
                    // Новый кадр начался раньше, чем собрался предыдущий.
                    framesDropped++;
                }
                pendingFrameId = frameId;
                pendingTotal = totalChunks;
                pendingChunks = new byte[totalChunks][];
                pendingReceived = 0;
                pendingWidth = width;
                pendingHeight = height;
                pendingTsMs = tsMs;
                pendingStartedAtMs = now;
            }
            if (chunkIndex >= pendingTotal) {
                framesBroken++;
                return;
            }
            if (pendingChunks[chunkIndex] == null) {
                byte[] chunk = new byte[payloadLen];
                System.arraycopy(buf, HEADER_LEN, chunk, 0, payloadLen);
                pendingChunks[chunkIndex] = chunk;
                pendingReceived++;
                chunksReceived++;
            }
            if (width > 0) {
                pendingWidth = width;
            }
            if (height > 0) {
                pendingHeight = height;
            }

            if (pendingReceived == pendingTotal) {
                assembleLocked();
            }
        }
    }

    /** Вызывается под {@link #lock}. Склеивает куски и декодирует JPEG. */
    private void assembleLocked() {
        int size = 0;
        for (int i = 0; i < pendingChunks.length; i++) {
            size += pendingChunks[i].length;
        }
        byte[] jpeg = new byte[size];
        int at = 0;
        for (int i = 0; i < pendingChunks.length; i++) {
            System.arraycopy(pendingChunks[i], 0, jpeg, at, pendingChunks[i].length);
            at += pendingChunks[i].length;
        }
        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        } catch (Throwable t) {
            Log.w(TAG, "JPEG не декодирован: " + t);
        }
        int doneFrameId = pendingFrameId;
        long doneTsMs = pendingTsMs;
        int w = pendingWidth;
        int h = pendingHeight;
        pendingFrameId = -1;

        if (bmp == null) {
            framesBroken++;
            return;
        }

        Bitmap old = latestBitmap;
        latestBitmap = bmp;
        latestWidth = w > 0 ? w : bmp.getWidth();
        latestHeight = h > 0 ? h : bmp.getHeight();
        latestSourceTsMs = doneTsMs;
        latestCompletedAtMs = System.currentTimeMillis();
        latestFrameId = doneFrameId;
        framesCompleted++;
        recentMs[recentAt % recentMs.length] = latestCompletedAtMs;
        recentAt++;
        if (old != null && !old.isRecycled()) {
            old.recycle();
        }
    }

    /** Кадр для отрисовки, или null — ничего ещё не собралось. */
    public Bitmap getLatestBitmap() {
        return latestBitmap;
    }

    public int getLatestWidth() {
        return latestWidth;
    }

    public int getLatestHeight() {
        return latestHeight;
    }

    /** Сколько миллисекунд назад собрался последний кадр, или -1. */
    public long getAgeMs(long nowMs) {
        return latestCompletedAtMs == 0 ? -1L : nowMs - latestCompletedAtMs;
    }

    /**
     * Ориентировочная задержка: разница часов ГУ и телефона на момент
     * последнего кадра, минус та же разница на первом кадре. Абсолютное
     * число не значимо — часы не синхронизированы, — значим только рост.
     */
    public long getLatencyDriftMs() {
        if (latestCompletedAtMs == 0) {
            return Long.MIN_VALUE;
        }
        return latestCompletedAtMs - latestSourceTsMs;
    }

    /** Кадров в секунду за последние ~16 завершённых кадров. */
    public float getFps() {
        int n = (int) Math.min(recentMs.length, framesCompleted);
        if (n < 2) {
            return 0f;
        }
        long newest = 0L;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            long v = recentMs[i];
            if (v > newest) {
                newest = v;
            }
            if (v < oldest) {
                oldest = v;
            }
        }
        long span = newest - oldest;
        return span <= 0 ? 0f : (n - 1) * 1000f / span;
    }

    public long getFramesCompleted() {
        return framesCompleted;
    }

    public long getFramesDropped() {
        return framesDropped;
    }

    public long getFramesBroken() {
        return framesBroken;
    }

    public long getLatestFrameId() {
        return latestFrameId;
    }

    private static int u16(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static int i32(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    private static long i64(byte[] b, int off) {
        long v = 0L;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (b[off + i] & 0xFFL);
        }
        return v;
    }
}
