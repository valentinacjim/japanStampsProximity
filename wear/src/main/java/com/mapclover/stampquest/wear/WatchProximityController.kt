package com.mapclover.stampquest.wear

import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.json.JSONArray
import kotlin.math.floor

/**
 * Builds a 1 km grid once, then checks just a 3x3 group of cells for each
 * location update. Only the selected Japan destinations are indexed.
 */
class WatchProximityController(private val context: Context) {
    private val stampsByCell: Map<Cell, List<WatchStamp>> by lazy { loadGrid() }
    private val notifiedIds = mutableSetOf<String>()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Eki stamps cercanos", NotificationManager.IMPORTANCE_HIGH
            ).apply { enableVibration(false) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun check(latitude: Double, longitude: Double): String? {
        val userCell = Cell.of(latitude, longitude)
        val nearby = (-1..1).asSequence().flatMap { latOffset ->
            (-1..1).asSequence().flatMap { lonOffset ->
                stampsByCell[Cell(userCell.latitude + latOffset, userCell.longitude + lonOffset)]
                    .orEmpty().asSequence()
            }
        }.firstOrNull { stamp ->
            distanceMeters(latitude, longitude, stamp.latitude, stamp.longitude) < ALERT_RADIUS_METERS
        } ?: return null

        if (notifiedIds.add(nearby.id)) alert(nearby)
        return nearby.name
    }

    fun testAlert() {
        alert(WatchStamp("test-vibration", "Prueba de vibración", 0.0, 0.0))
    }

    private fun loadGrid(): Map<Cell, List<WatchStamp>> {
        val json = context.assets.open("mapa.json").bufferedReader().use { it.readText() }
        val entries = JSONArray(json)
        val grid = mutableMapOf<Cell, MutableList<WatchStamp>>()
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val coordinates = entry.optJSONObject("coordenadas") ?: continue
            val latitude = coordinates.optDouble("latitud", Double.NaN)
            val longitude = coordinates.optDouble("longitud", Double.NaN)
            if (!latitude.isFinite() || !longitude.isFinite() ||
                (entry.optString("id") != TEST_STAMP_ID && !isSupportedArea(latitude, longitude))
            ) continue
            val stamp = WatchStamp(entry.optString("id"), entry.optString("nombre_en"), latitude, longitude)
            grid.getOrPut(Cell.of(latitude, longitude)) { mutableListOf() }.add(stamp)
        }
        return grid
    }

    private fun alert(stamp: WatchStamp) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Eki stamp cerca")
            .setContentText("A menos de 100 m de ${stamp.name}")
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(stamp.id.hashCode(), notification)
    }

    private fun isSupportedArea(latitude: Double, longitude: Double) = supportedAreas.any {
        latitude in it.minLatitude..it.maxLatitude && longitude in it.minLongitude..it.maxLongitude
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(latDistance / 2) * kotlin.math.sin(latDistance / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(lonDistance / 2) * kotlin.math.sin(lonDistance / 2)
        return 6_371_000 * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    private data class WatchStamp(val id: String, val name: String, val latitude: Double, val longitude: Double)
    private data class Cell(val latitude: Int, val longitude: Int) {
        companion object {
            // Approx. 1 km cells; sufficiently precise for the 100 m alert radius.
            fun of(latitude: Double, longitude: Double) = Cell(floor(latitude / 0.009).toInt(), floor(longitude / 0.011).toInt())
        }
    }
    private data class Area(val minLatitude: Double, val maxLatitude: Double, val minLongitude: Double, val maxLongitude: Double)

    private companion object {
        const val CHANNEL_ID = "nearby_eki_stamps"
        const val ALERT_RADIUS_METERS = 100.0
        const val TEST_STAMP_ID = "test-felipe-pingarron-5a"
        val supportedAreas = listOf(
            Area(35.52, 35.90, 139.55, 139.95), Area(34.85, 35.15, 135.55, 135.95),
            Area(35.15, 35.30, 138.90, 139.20), Area(36.05, 36.25, 137.10, 137.35),
            Area(35.30, 35.50, 136.65, 136.90), Area(34.55, 34.85, 135.35, 135.75),
            Area(36.55, 37.10, 139.45, 140.05), Area(35.90, 36.10, 139.65, 139.85),
            Area(35.25, 35.40, 139.40, 139.60)
        )
    }
}
