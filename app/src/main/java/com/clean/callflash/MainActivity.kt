package com.clean.callflash

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Anmod automatisk om tilladelser ved åbning
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
        )
        val missingPermissions = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), 101)
        }

        // Simpel brugerflade
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }

        val statusText = TextView(this).apply {
            text = "CallFlash Aktiv\nAppen er nu aktiveret og klar i baggrunden."
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }

        val testButton = Button(this).apply {
            text = "Test Fotolys (Blinker 3 gange)"
            setOnClickListener {
                testFlashlight()
            }
        }

        layout.addView(statusText)
        layout.addView(testButton)
        setContentView(layout)
    }

    private fun testFlashlight() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = CallReceiver.getFlashCameraId(cameraManager) ?: return

        scope.launch(Dispatchers.Default) {
            repeat(3) {
                cameraManager.setTorchMode(cameraId, true)
                delay(250)
                cameraManager.setTorchMode(cameraId, false)
                delay(250)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
