package com.mapclover.stampquest.ui.map

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapclover.stampquest.domain.service.ProximityService
import com.mapclover.stampquest.notification.ProximityNotifier
import kotlin.collections.remove
import kotlin.math.roundToInt

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val repository = remember { KmlRepository(context) }
    val stamps = remember { repository.loadStamps() }
    val notifier = remember { ProximityNotifier(context) }
    val proximityService = remember { ProximityService(context, notifier) }

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        MapView(context).apply {
            setMultiTouchControls(true)
            val locationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(context),
                this
            )
            locationOverlay.enableMyLocation()
            overlays.add(locationOverlay)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            val startPoint = GeoPoint(40.4168, -3.7038)
            map.controller.setZoom(6.0)
            map.controller.setCenter(startPoint)

            map.overlays.filterIsInstance<Marker>().forEach { map.overlays.remove(it) }

            val locationOverlay = map.overlays
                .filterIsInstance<MyLocationNewOverlay>()
                .firstOrNull()

            val stampInfoWindow = makeStampInfoWindow(context, map, locationOverlay)

            stamps.forEach { stamp ->
                val marker = Marker(map)
                val label = stamp.name.take(1).uppercase()
                val iconDrawable = BitmapDrawable(
                    context.resources,
                    createCircularIcon(80, Color.parseColor("#FF7043"), label)
                )
                marker.icon = iconDrawable
                marker.title = stamp.name + if (!stamp.englishName.isNullOrBlank()) " (${stamp.englishName})" else ""
                marker.snippet = stamp.address ?: ""
                marker.position = GeoPoint(stamp.latitude, stamp.longitude)
                marker.relatedObject = stamp
                marker.infoWindow = stampInfoWindow
                marker.setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    true
                }
                map.overlays.add(marker)
            }

            val myLocation = locationOverlay?.myLocation
            if (myLocation != null) {
                proximityService.checkProximity(myLocation, stamps)
            }
        }
    )
}

/**
 * Crea una InfoWindow personalizada que muestra título, dirección y distancia (si hay ubicación).
 * Se construye con vistas en código para evitar dependencia de layouts XML.
 */
private fun makeStampInfoWindow(
    context: Context,
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay?
): InfoWindow {
    // Crear layout programáticamente
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 16, 24, 16)
        setBackgroundColor(Color.WHITE)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        elevation = 8f
    }

    val titleView = TextView(context).apply {
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.BLACK)
        textSize = 16f
    }

    val addressView = TextView(context).apply {
        setTextColor(Color.DKGRAY)
        textSize = 14f
    }

    val distanceView = TextView(context).apply {
        setTextColor(Color.GRAY)
        textSize = 12f
        gravity = Gravity.END
    }

    container.addView(titleView)
    container.addView(addressView)
    container.addView(distanceView)

    return object : InfoWindow(container, mapView) {
        override fun onOpen(item: Any?) {
            val marker = item as? Marker ?: return
            val stamp = marker.relatedObject

            titleView.text = marker.title ?: ""
            addressView.text = marker.snippet ?: ""

            // Calcular distancia si hay ubicación conocida
            val myLocation = locationOverlay?.myLocation
            val distanceText = if (myLocation != null && stamp is com.mapclover.stampquest.data.model.Stamp) {
                val d = myLocation.distanceToAsDouble(GeoPoint(stamp.latitude, stamp.longitude))
                "${d.roundToInt()} m"
            } else {
                ""
            }
            distanceView.text = distanceText
        }

        override fun onClose() {
            // noop
        }
    }
}

/**
 * Genera un bitmap circular con una letra en el centro para usar de icono.
 * Así no dependemos de drawable resources.
 */
private fun createCircularIcon(sizePx: Int, color: Int, letter: String): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius, paint)

    // Texto
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = sizePx * 0.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val fm = textPaint.fontMetrics
    val textY = radius - (fm.ascent + fm.descent) / 2
    canvas.drawText(letter, radius, textY, textPaint)

    return bmp
}
