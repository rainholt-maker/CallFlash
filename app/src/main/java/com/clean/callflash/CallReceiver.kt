package com.clean.callflash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class CallReceiver : BroadcastReceiver() {

    companion object {
        private val isRinging = AtomicBoolean(false)
        private var flashJob: Job? = null

        fun getFlashCameraId(cameraManager: CameraManager): String? {
            return try {
                cameraManager.cameraIdList.firstOrNull { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            } catch (e: Exception) {
                null
            }
        }

        fun stopFlashing(cameraManager: CameraManager, cameraId: String?) {
            isRinging.set(false)
            flashJob?.cancel()
            flashJob = null
            if (cameraId != null) {
                try {
                    cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = getFlashCameraId(cameraManager) ?: return

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Hvis den allerede blinker, start ikke et nyt loop
                if (isRinging.getAndSet(true)) return

                val pendingResult = goAsync()
                flashJob = CoroutineScope(Dispatchers.Default).launch {
                    try {
                        while (isRinging.get() && isActive) {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(300)
                            if (!isRinging.get() || !isActive) break
                            cameraManager.setTorchMode(cameraId, false)
                            delay(300)
                        }
                    } catch (_: Exception) {
                    } finally {
                        try {
                            cameraManager.setTorchMode(cameraId, false)
                        } catch (_: Exception) {}
                        pendingResult.finish()
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK, TelephonyManager.EXTRA_STATE_IDLE -> {
                // Stopper loopet med det samme, uanset om opkaldet besvares eller afsluttes
                stopFlashing(cameraManager, cameraId)
            }
        }
    }
}
