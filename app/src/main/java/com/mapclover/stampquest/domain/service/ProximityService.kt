package com.mapclover.stampquest.domain.service

import android.content.Context
import com.mapclover.stampquest.data.model.Stamp
import com.mapclover.stampquest.notification.ProximityNotifier
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class ProximityService(
    private val context: Context,
    private val notifier: ProximityNotifier
) {
    private val vibratedStamps = mutableSetOf<String>()

    fun checkProximity(
        userLocation: GeoPoint?,
        stamps: List<Stamp>,
        radiusMeters: Float = 50f
    ) {
        if (userLocation == null) return

        stamps.forEach { stamp ->
            val distance = userLocation.distanceToAsDouble(
                GeoPoint(stamp.latitude, stamp.longitude)
            )

            if (distance < radiusMeters && !vibratedStamps.contains(stamp.id)) {
                notifier.vibrateUnlock()
                vibratedStamps.add(stamp.id)
            }
        }
    }

    fun resetStamp(stampId: String) {
        vibratedStamps.remove(stampId)
    }

    fun reset() {
        vibratedStamps.clear()
    }
}
