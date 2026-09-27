package com.q50gtr.alpbridge

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.q50gtr.alpbridge.databinding.ActivityMainBinding

/**
 * Единственный экран компаньона.
 *
 * Разрешение MediaProjection у пользователя спрашивает система — этот экран
 * только его запускает и передаёт результат службе. Ни здесь, ни в
 * MirrorService нет пути начать трансляцию без этого диалога: платформа
 * не даёт его обойти.
 */
class MainActivity : AppCompatActivity(), MirrorService.StatusListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private var mirroring = false

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            MirrorService.pendingResultCode = result.resultCode
            MirrorService.pendingResultData = data
            startForegroundService(Intent(this, MirrorService::class.java))
            mirroring = true
            binding.btnStartStop.setText(R.string.btn_stop)
        }
    }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* отказ просто не покажет уведомление; служба всё равно работает */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences(MirrorService.PREFS, MODE_PRIVATE)

        binding.cropLeft.setText(prefs.getFloat(MirrorService.KEY_CROP_LEFT, 0f).toString())
        binding.cropTop.setText(prefs.getFloat(MirrorService.KEY_CROP_TOP, 0f).toString())
        binding.cropRight.setText(prefs.getFloat(MirrorService.KEY_CROP_RIGHT, 1f).toString())
        binding.cropBottom.setText(prefs.getFloat(MirrorService.KEY_CROP_BOTTOM, 1f).toString())

        binding.btnCropFull.setOnClickListener {
            binding.cropLeft.setText("0.0")
            binding.cropTop.setText("0.0")
            binding.cropRight.setText("1.0")
            binding.cropBottom.setText("1.0")
            saveCrop()
        }
        binding.cropLeft.doAfterTextChangedSafe { saveCrop() }
        binding.cropTop.doAfterTextChangedSafe { saveCrop() }
        binding.cropRight.doAfterTextChangedSafe { saveCrop() }
        binding.cropBottom.doAfterTextChangedSafe { saveCrop() }

        // Переключатель управления показан, но выключен: третий этап задачи
        // остаётся исследованным и готовым (TapAccessibilityService), а не
        // включённым — до подтверждённого первого теста трансляции и до
        // проверки сигнала стоянки (docs/ALP-CONTROL.md). Включать его в
        // коде раньше времени значило бы нарушить именно то ограничение,
        // которое было явно поставлено в задаче.
        binding.switchControl.isChecked = false
        binding.switchControl.isEnabled = false

        binding.btnStartStop.setOnClickListener {
            if (mirroring) {
                stopMirroring()
            } else {
                requestNotificationPermissionIfNeeded()
                val mgr = getSystemService(MediaProjectionManager::class.java)
                projectionLauncher.launch(mgr.createScreenCaptureIntent())
            }
        }

        MirrorService.listener = this
        MirrorService.lastDcuAddress?.let { onTarget(it) }
    }

    private fun saveCrop() {
        val l = binding.cropLeft.text?.toString()?.toFloatOrNull() ?: return
        val t = binding.cropTop.text?.toString()?.toFloatOrNull() ?: return
        val r = binding.cropRight.text?.toString()?.toFloatOrNull() ?: return
        val b = binding.cropBottom.text?.toString()?.toFloatOrNull() ?: return
        prefs.edit()
            .putFloat(MirrorService.KEY_CROP_LEFT, l.coerceIn(0f, 1f))
            .putFloat(MirrorService.KEY_CROP_TOP, t.coerceIn(0f, 1f))
            .putFloat(MirrorService.KEY_CROP_RIGHT, r.coerceIn(0f, 1f))
            .putFloat(MirrorService.KEY_CROP_BOTTOM, b.coerceIn(0f, 1f))
            .apply()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun stopMirroring() {
        stopService(Intent(this, MirrorService::class.java))
        mirroring = false
        binding.btnStartStop.setText(R.string.btn_start)
    }

    override fun onTarget(address: String?) {
        runOnUiThread {
            binding.textTarget.text = getString(R.string.label_target) + " " +
                (address ?: getString(R.string.label_target_none))
        }
    }

    override fun onStats(framesSent: Long, kbytesPerSec: Float, fps: Float) {
        runOnUiThread {
            binding.textStats.text = "кадров: $framesSent   ${"%.1f".format(kbytesPerSec)} КБ/с   цель ${fps.toInt()} fps"
        }
    }

    override fun onStopped() {
        runOnUiThread {
            mirroring = false
            binding.btnStartStop.setText(R.string.btn_start)
        }
    }

    override fun onDestroy() {
        if (MirrorService.listener === this) {
            MirrorService.listener = null
        }
        super.onDestroy()
    }
}

/** Маленький помощник без лишней зависимости от androidx.core ktx-виджетов. */
private fun com.google.android.material.textfield.TextInputEditText.doAfterTextChangedSafe(
    action: () -> Unit,
) {
    addTextChangedListener(object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) = action()
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
