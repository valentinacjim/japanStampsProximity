package com.mapclover.stampquest.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Keeps proximity checks running on the watch while its screen is off. */
class WatchProximityService : Service() {
    private lateinit var tracker: WatchLocationTracker
    private lateinit var proximity: WatchProximityController

    override fun onCreate() {
        super.onCreate()
        createTrackingChannel()
        val notification = Notification.Builder(this, TRACKING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Eki Stamps activos")
            .setContentText("Buscando sellos cercanos")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        proximity = WatchProximityController(this)
        tracker = WatchLocationTracker(this)
        tracker.start { location ->
            proximity.check(location.latitude, location.longitude)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        tracker.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createTrackingChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            TRACKING_CHANNEL_ID, "Seguimiento de Eki Stamps", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TRACKING_CHANNEL_ID = "eki_stamp_tracking"
        private const val NOTIFICATION_ID = 100

        fun intent(context: Context) = Intent(context, WatchProximityService::class.java)
    }
}
