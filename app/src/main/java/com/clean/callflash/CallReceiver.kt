package com.clean.callflash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.*

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var flashJob: Job? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = try {
            cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            return
        }

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (flashJob?.isActive == true) return
                
                flashJob = CoroutineScope(Dispatchers.Default).launch {
                    try {
                        while (isActive) {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(300)
                            cameraManager.setTorchMode(cameraId, false)
                            delay(300)
                        }
                    } catch (e: Exception) {
                        try { cameraManager.setTorchMode(cameraId, false) } catch (_: Exception) {}
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK, TelephonyManager.EXTRA_STATE_IDLE -> {
                flashJob?.cancel()
                try {
                    cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            }
        }
    }
}
