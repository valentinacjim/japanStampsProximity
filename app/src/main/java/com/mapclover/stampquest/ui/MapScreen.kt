package com.mapclover.stampquest.ui


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mapclover.stampquest.data.repository.KmlRepository
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


@Composable
fun MapScreen() {
    val context = LocalContext.current
    val repository = remember { KmlRepository(context) }
    val stamps = remember { repository.loadStamps() }

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        MapView(context).apply {
            setMultiTouchControls(true)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            val startPoint = GeoPoint(40.4168, -3.7038)
            map.controller.setZoom(6.0)
            map.controller.setCenter(startPoint)

            stamps.forEach { stamp ->
                val marker = Marker(map)
                marker.position = GeoPoint(stamp.latitude, stamp.longitude)
                marker.title += if (stamp.englishName != null) " (${stamp.englishName})" else " (${stamp.name})"
                marker.snippet = stamp.address ?: ""

                marker.setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    true
                }

                map.overlays.add(marker)
            }
        }
    )
}
