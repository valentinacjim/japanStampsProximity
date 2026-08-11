package com.mapclover.stampquest.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.mapclover.stampquest.data.repository.JsonRepository
import com.mapclover.stampquest.domain.service.ProximityService
import com.mapclover.stampquest.notification.ProximityNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/** Keeps nearby-stamp alerts active after the map is no longer on screen. */
class ProximityTrackingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var tracker: EkiProximityLocationTracker

    override fun onCreate() {
        super.onCreate()
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        tracker = EkiProximityLocationTracker(this)
        val proximityService = ProximityService(this, ProximityNotifier(this))
        serviceScope.launch {
            val stamps = JsonRepository(this@ProximityTrackingService).loadStamps()
            tracker.start { location ->
                proximityService.checkProximity(GeoPoint(location.latitude, location.longitude), stamps)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        tracker.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Alertas de Eki Stamps activas")
            .setContentText("Buscando sellos cercanos")
            .setOngoing(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Seguimiento de Eki Stamps",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "eki_stamp_tracking"
        private const val NOTIFICATION_ID = 100

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ProximityTrackingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProximityTrackingService::class.java))
        }
    }
}
