package com.q50gtr.alpbridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Исполняет один жест по нормированным координатам — и больше ничего.
 *
 * Третий этап задачи ("ИССЛЕДОВАТЬ ВОЗМОЖНОСТЬ") реализован здесь как
 * рабочий, но выключенный по умолчанию путь:
 *
 *  - службу нужно включить вручную в Настройках -> Специальные возможности —
 *    приложение не может сделать это само, и не пытается;
 *  - в приложении отдельный переключатель "Разрешить управление" должен
 *    быть включён отдельно (см. MainActivity, по умолчанию заблокирован);
 *  - и даже при обоих условиях ГУ в этой поставке не отправляет ни одной
 *    команды касания — protocol.parseTap() в MirrorService разбирает их,
 *    но отправка на стороне DiagOverlay нигде не вызывается.
 *
 * Значит первый физический тест (второй этап, только просмотр) идёт без
 * какого-либо риска случайного нажатия. Включать управление раньше, чем
 * этот тест пройден и подтверждён живым сигналом стоянки (см.
 * docs/ALP-CONTROL.md), не нужно и не предполагается.
 *
 * canRetrieveWindowContent выключен в конфиге службы: ей не нужно читать
 * содержимое экрана, только выполнять жест по уже известным координатам.
 */
class TapAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "Q50AlpBridge/Tap"

        @Volatile
        var instance: TapAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "служба касаний подключена (управление всё ещё требует отдельного переключателя)")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Содержимое окон эта служба не разбирает и разбирать не должна.
    }

    override fun onInterrupt() {}

    /**
     * @param nx доля 0..1 по горизонтали от кадра, показанного на ГУ
     * @param ny доля 0..1 по вертикали
     * @param longPress true — долгое нажатие вместо короткого тапа
     */
    fun dispatchNormalizedTap(nx: Float, ny: Float, longPress: Boolean) {
        val crop = MirrorService.cropRectCapturePx
        val scale = MirrorService.captureScale
        if (crop == null || scale <= 0f) {
            Log.w(TAG, "нет геометрии захвата — жест не выполнен")
            return
        }
        val capX = crop.left + nx * crop.width()
        val capY = crop.top + ny * crop.height()
        val screenX = capX / scale
        val screenY = capY / scale

        val path = Path().apply { moveTo(screenX, screenY) }
        val duration = if (longPress) 600L else 60L
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
