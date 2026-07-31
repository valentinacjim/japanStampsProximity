package com.mapclover.stampquest.wear

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WearMainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var proximityTester: WatchProximityController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(20, 20, 20, 20)
            text = "Preparando Eki Stamps…"
        }
        proximityTester = WatchProximityController(this)
        val testButton = Button(this).apply {
            text = "Probar vibración"
            setOnClickListener {
                proximityTester.testAlert()
                status.text = "Prueba enviada. ¿Ha vibrado el reloj?"
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(status)
            addView(testButton)
        })

        requestPermissionsIfNeeded()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startTracking()
    }

    private fun startTracking() {
        if (!hasLocationPermission()) {
            status.text = "Permite la ubicación para avisarte de los eki stamps cercanos."
            return
        }
        ContextCompat.startForegroundService(this, WatchProximityService.intent(this))
        status.text = "Seguimiento activo. Te avisaré al acercarte a un eki stamp."
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        } else {
            // The user may already have granted permissions in a previous install.
            // Start tracking in that case instead of waiting for a callback that won't arrive.
            startTracking()
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
