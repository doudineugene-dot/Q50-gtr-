package com.q50gtr.alpbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.DatagramPacket
import android.net.DatagramSocket
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.math.min

/**
 * Захват экрана и передача его как JPEG-кадров на ГУ по UDP.
 *
 * Разрешение MediaProjection пользователь даёт системным диалогом — это
 * нельзя ни обойти, ни выдать без него, и заново спрашивается каждый раз,
 * когда служба перезапускается (так требует сама платформа). Здесь оно
 * только используется: результат уже подтверждён в MainActivity.
 *
 * "Только нужную часть экрана" в задаче — без accessibility-доступа к
 * границам окна ALP3 автоматически вырезать нельзя: Android не даёт
 * стороннему приложению узнать прямоугольник чужого окна без разрешения на
 * чтение содержимого экрана, а именно от него по возможности стоит держаться
 * подальше — оно даёт значительно больше, чем нужно для одной трансляции.
 * Поэтому здесь единственный безопасный путь — ручная настройка
 * прямоугольника обрезки в MainActivity (доли экрана 0..1): пользователь
 * один раз наводит рамку под то место, где на его экране рисуется ALP3, и
 * дальше она не меняется, пока он её не подвинет сам.
 */
class MirrorService : Service() {

    companion object {
        private const val TAG = "Q50AlpBridge/Mirror"
        private const val CHANNEL_ID = "alp_mirror"
        private const val NOTIFICATION_ID = 1

        /** Результат системного диалога разрешения — передаётся из
         *  MainActivity в момент запуска, а не через Intent extras (Intent с
         *  данными о проекции нельзя просто положить в обычный extra). */
        var pendingResultCode: Int = 0
        var pendingResultData: Intent? = null

        /** Максимальная ширина захвата — экран ГУ 800x480, лишние пиксели
         *  только увеличивают JPEG и задержку без выигрыша в читаемости. */
        const val CAPTURE_MAX_WIDTH = 960

        const val TARGET_FPS = 4

        const val PREFS = "q50_alp_bridge"
        const val KEY_CROP_LEFT = "crop_left"
        const val KEY_CROP_TOP = "crop_top"
        const val KEY_CROP_RIGHT = "crop_right"
        const val KEY_CROP_BOTTOM = "crop_bottom"
        const val KEY_CONTROL_ENABLED = "control_enabled"

        /** Слушатель для MainActivity — статус трансляции на одном экране,
         *  без межпроцессного IPC ради простоты (служба живёт в том же
         *  процессе, что и активность). */
        @Volatile
        var listener: StatusListener? = null

        /** Последний известный адрес ГУ — из маяка. Читает TapAccessibilityService
         *  не требуется; читает MainActivity для отображения. */
        @Volatile
        var lastDcuAddress: String? = null

        /*
         * Геометрия последнего захвата — нужна только TapAccessibilityService,
         * чтобы перевести нормированные координаты касания (0..1 от кадра,
         * который реально показан на ГУ) обратно в пиксели настоящего экрана
         * телефона: кадр может быть уменьшен (CAPTURE_MAX_WIDTH) и обрезан
         * (ручная рамка пользователя), и оба сдвига нужно отменить по
         * порядку. Ничего не читает и не использует, пока ГУ не начнёт
         * слать команды касания — а в этой поставке оно их не шлёт.
         */
        @Volatile var captureScale = 1f
        @Volatile var cropRectCapturePx: Rect? = null
    }

    interface StatusListener {
        fun onTarget(address: String?)
        fun onStats(framesSent: Long, kbytesPerSec: Float, fps: Float)
        fun onStopped()
    }

    private var projectionManager: MediaProjectionManager? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var socket: DatagramSocket? = null
    private var receiveThread: Thread? = null
    @Volatile private var running = false

    @Volatile private var targetAddress: InetAddress? = null
    @Volatile private var lastFrameAtMs = 0L
    private var frameId = 0
    @Volatile private var framesSent = 0L
    @Volatile private var bytesSentWindow = 0L
    @Volatile private var windowStartMs = 0L

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        projectionManager = getSystemService(MediaProjectionManager::class.java)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!running) {
            startCapture()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL_ID, getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        nm.createNotificationChannel(ch)
    }

    private fun startCapture() {
        val resultCode = pendingResultCode
        val data = pendingResultData
        if (data == null) {
            Log.w(TAG, "нет разрешения MediaProjection — служба не может захватывать экран")
            stopSelf()
            return
        }
        val projection = projectionManager?.getMediaProjection(resultCode, data) ?: run {
            Log.w(TAG, "getMediaProjection вернул null")
            stopSelf()
            return
        }
        this.projection = projection
        // Android 14+ требует зарегистрировать колбэк ДО createVirtualDisplay
        // для проекции всего экрана — иначе платформа бросает исключение.
        // Он же даёт узнать, что пользователь остановил трансляцию из
        // системной шторки, а не только из этого приложения.
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "MediaProjection остановлена системой")
                stopSelf()
            }
        }, null)

        val metrics = DisplayMetrics()
        val wm = getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val density = metrics.densityDpi
        val srcW = metrics.widthPixels
        val srcH = metrics.heightPixels
        val scale = min(1f, CAPTURE_MAX_WIDTH.toFloat() / srcW)
        val capW = (srcW * scale).toInt().coerceAtLeast(2) and 1.inv()
        val capH = (srcH * scale).toInt().coerceAtLeast(2) and 1.inv()
        captureScale = scale

        val reader = ImageReader.newInstance(capW, capH, android.graphics.PixelFormat.RGBA_8888, 2)
        imageReader = reader

        virtualDisplay = projection.createVirtualDisplay(
            "Q50AlpBridge",
            capW, capH, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null,
        )

        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(AlpProtocol.PORT))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "порт ${AlpProtocol.PORT} занят: $t")
        }

        running = true
        windowStartMs = System.currentTimeMillis()
        startReceiveLoop()

        val frameIntervalMs = 1000L / TARGET_FPS
        reader.setOnImageAvailableListener({ r ->
            val now = System.currentTimeMillis()
            val img = r.acquireLatestImage()
            if (img == null) {
                return@setOnImageAvailableListener
            }
            if (!running || now - lastFrameAtMs < frameIntervalMs) {
                img.close()
                return@setOnImageAvailableListener
            }
            lastFrameAtMs = now
            try {
                encodeAndSend(img, capW, capH)
            } catch (t: Throwable) {
                Log.w(TAG, "кадр не отправлен: $t")
            } finally {
                img.close()
            }
        }, null)
    }

    private fun encodeAndSend(image: Image, capW: Int, capH: Int) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * capW

        var bmp = Bitmap.createBitmap(
            capW + rowPadding / pixelStride, capH, Bitmap.Config.ARGB_8888,
        )
        bmp.copyPixelsFromBuffer(buffer)
        if (bmp.width != capW) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, capW, capH)
        }

        val crop = readCrop()
        val cropRect = Rect(
            (crop[0] * capW).toInt(),
            (crop[1] * capH).toInt(),
            (crop[2] * capW).toInt().coerceAtMost(capW),
            (crop[3] * capH).toInt().coerceAtMost(capH),
        )
        cropRectCapturePx = cropRect
        val cropped = if (cropRect.left == 0 && cropRect.top == 0 &&
            cropRect.width() == capW && cropRect.height() == capH
        ) {
            bmp
        } else if (cropRect.width() > 0 && cropRect.height() > 0) {
            Bitmap.createBitmap(bmp, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
        } else {
            bmp
        }

        val out = ByteArrayOutputStream(32 * 1024)
        cropped.compress(Bitmap.CompressFormat.JPEG, 55, out)
        val jpeg = out.toByteArray()

        sendFrame(jpeg, cropped.width, cropped.height)

        if (cropped !== bmp) {
            bmp.recycle()
        }
        cropped.recycle()
    }

    private fun readCrop(): FloatArray {
        val l = prefs.getFloat(KEY_CROP_LEFT, 0f).coerceIn(0f, 1f)
        val t = prefs.getFloat(KEY_CROP_TOP, 0f).coerceIn(0f, 1f)
        val r = prefs.getFloat(KEY_CROP_RIGHT, 1f).coerceIn(l, 1f)
        val b = prefs.getFloat(KEY_CROP_BOTTOM, 1f).coerceIn(t, 1f)
        return floatArrayOf(l, t, r, b)
    }

    private fun sendFrame(jpeg: ByteArray, width: Int, height: Int) {
        val target = targetAddress ?: return
        val sock = socket ?: return
        val tsMs = System.currentTimeMillis()
        val fid = frameId++
        val total = (jpeg.size + AlpProtocol.MAX_CHUNK_PAYLOAD - 1) / AlpProtocol.MAX_CHUNK_PAYLOAD
        var sent = 0
        var idx = 0
        var bytesThisFrame = 0
        while (sent < jpeg.size) {
            val len = min(AlpProtocol.MAX_CHUNK_PAYLOAD, jpeg.size - sent)
            val packetBytes = AlpProtocol.buildFrameChunk(
                fid, total, idx, width, height, tsMs, jpeg, sent, len,
            )
            try {
                sock.send(DatagramPacket(packetBytes, packetBytes.size, target, AlpProtocol.PORT))
                bytesThisFrame += packetBytes.size
            } catch (t: Throwable) {
                Log.w(TAG, "кусок $idx/$total кадра $fid не отправлен: $t")
            }
            sent += len
            idx++
        }
        framesSent++
        bytesSentWindow += bytesThisFrame
        val now = System.currentTimeMillis()
        val elapsed = now - windowStartMs
        if (elapsed >= 1000) {
            val kbps = bytesSentWindow / 1024f / (elapsed / 1000f)
            listener?.onStats(framesSent, kbps, TARGET_FPS.toFloat())
            bytesSentWindow = 0
            windowStartMs = now
        }
    }

    private fun startReceiveLoop() {
        val sock = socket ?: return
        receiveThread = Thread({
            val buf = ByteArray(2048)
            while (running) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    sock.receive(packet)
                } catch (t: Throwable) {
                    if (running) Log.w(TAG, "приём: $t")
                    break
                }
                handleIncoming(buf, packet.length)
            }
        }, "q50-alp-recv")
        receiveThread?.isDaemon = true
        receiveThread?.start()
    }

    private fun handleIncoming(buf: ByteArray, len: Int) {
        // Маяк — текст: "Q50GTR BEACON <ip> PORT <port>". Кадры-от-нас на
        // этот же порт эхом не приходят (широковещательные адреса из
        // targetAddress мы себе не шлём), поэтому попытка разобрать как
        // текст безопасна для всего, что не начинается с двоичной сигнатуры.
        if (len >= AlpProtocol.MAGIC_TAP.size && matches(buf, AlpProtocol.MAGIC_TAP)) {
            val tap = AlpProtocol.parseTap(buf, len) ?: return
            if (prefs.getBoolean(KEY_CONTROL_ENABLED, false)) {
                TapAccessibilityService.instance?.dispatchNormalizedTap(tap.nx, tap.ny, tap.longPress)
            }
            return
        }
        val text = try {
            String(buf, 0, len, Charsets.UTF_8)
        } catch (t: Throwable) {
            return
        }
        val beacon = AlpProtocol.parseBeacon(text) ?: return
        try {
            targetAddress = InetAddress.getByName(beacon.dcuAddress)
            lastDcuAddress = beacon.dcuAddress
            listener?.onTarget(beacon.dcuAddress)
        } catch (t: Throwable) {
            Log.w(TAG, "адрес маяка не разобран: $t")
        }
    }

    private fun matches(buf: ByteArray, magic: ByteArray): Boolean {
        for (i in magic.indices) {
            if (buf[i] != magic[i]) return false
        }
        return true
    }

    override fun onDestroy() {
        running = false
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        socket?.close()
        listener?.onStopped()
        super.onDestroy()
    }
}
