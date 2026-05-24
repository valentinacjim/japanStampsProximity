package com.mapclover.stampquest.notification

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ProximityNotifier(
    private val context: Context
) {

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
}