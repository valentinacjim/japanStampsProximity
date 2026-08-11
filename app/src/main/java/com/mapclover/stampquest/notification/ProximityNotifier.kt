package com.mapclover.stampquest.notification

import android.Manifest
import android.R
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mapclover.stampquest.data.model.Stamp

class ProximityNotifier(
    private val context: Context
) {

    init {
        createNotificationChannel()
    }

    fun notifyNearbyStamp(stamp: Stamp) {
        vibrateUnlock()

        val canPostNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!canPostNotifications) return

        val notification = NotificationCompat.Builder(context, PROXIMITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_map)
            .setContentTitle("Eki stamp cerca")
            .setContentText("Estás a menos de 100 m de ${stamp.nombreEn}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(stamp.id.hashCode(), notification)
    }

    fun vibrateUnlock() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val vibratorManager =
                context.getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE
                ) as VibratorManager

            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            @Suppress("DEPRECATION")
            val vibrator =
                context.getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            PROXIMITY_CHANNEL_ID,
            "Eki stamps cercanos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos al acercarte a un eki stamp"
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private companion object {
        const val PROXIMITY_CHANNEL_ID = "nearby_eki_stamps"
    }
}
