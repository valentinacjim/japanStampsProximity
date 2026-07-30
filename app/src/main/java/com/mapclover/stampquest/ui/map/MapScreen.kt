package com.mapclover.stampquest.ui.map

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.DisposableEffect
import com.mapclover.stampquest.data.repository.JsonRepository
import com.mapclover.stampquest.domain.service.ProximityService
import com.mapclover.stampquest.notification.ProximityNotifier
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import kotlin.math.roundToInt

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val repository = remember { JsonRepository(context) }
    val stamps = androidx.compose.runtime.produceState(
        initialValue = emptyList<com.mapclover.stampquest.data.model.Stamp>(),
        repository
    ) {
        value = repository.loadStamps()
    }.value
    val notifier = remember { ProximityNotifier(context) }
    val proximityService = remember { ProximityService(context, notifier) }
    val markerRenderer = remember(context) { MapMarkerRenderer(context) }

    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(8.0)
            controller.setCenter(GeoPoint(35.6762, 139.6503))
            val locationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(context),
                this
            )
            locationOverlay.enableMyLocation()
            overlays.add(locationOverlay)
            markerRenderer.attach(this, locationOverlay)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            markerRenderer.dispose()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            markerRenderer.setStamps(stamps)
            val myLocation = markerRenderer.locationOverlay?.myLocation
            if (myLocation != null) {
                proximityService.checkProximity(myLocation, stamps)
            }
        }
    )
}

/** Keeps map overlays outside Compose state and only creates markers in the viewport. */
private class MapMarkerRenderer(private val context: Context) : MapListener {
    private lateinit var map: MapView
    var locationOverlay: MyLocationNewOverlay? = null
        private set
    private var stamps: List<com.mapclover.stampquest.data.model.Stamp> = emptyList()
    private val markers = mutableListOf<Marker>()
    private val iconBitmaps = mutableMapOf<String, Bitmap>()
    private var lastViewportKey: String? = null
    private val renderHandler = Handler(Looper.getMainLooper())
    private val deferredRender = Runnable { renderVisibleMarkers() }

    fun attach(map: MapView, locationOverlay: MyLocationNewOverlay) {
        this.map = map
        this.locationOverlay = locationOverlay
        map.addMapListener(this)
    }

    fun setStamps(stamps: List<com.mapclover.stampquest.data.model.Stamp>) {
        if (this.stamps === stamps) return
        this.stamps = stamps
        lastViewportKey = null
        renderVisibleMarkers()
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        scheduleRender()
        return false
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        scheduleRender()
        return false
    }

    fun dispose() {
        renderHandler.removeCallbacks(deferredRender)
        if (::map.isInitialized) map.removeMapListener(this)
    }

    private fun scheduleRender() {
        // Map events arrive for every small movement; wait until the gesture settles
        // instead of scanning the full data set on every frame.
        renderHandler.removeCallbacks(deferredRender)
        renderHandler.postDelayed(deferredRender, 150)
    }

    private fun renderVisibleMarkers() {
        if (!::map.isInitialized || stamps.isEmpty()) return
        val box = map.boundingBox
        val viewportKey = "${box.latNorth}:${box.latSouth}:${box.lonEast}:${box.lonWest}"
        if (viewportKey == lastViewportKey) return
        lastViewportKey = viewportKey

        val visibleStamps = stamps.asSequence().filter { stamp ->
            val lat = stamp.lat
            val lon = stamp.lon
            lat != null && lon != null &&
                lat in box.latSouth..box.latNorth && lon in box.lonWest..box.lonEast
        }.toList()

        map.overlays.removeAll(markers)
        markers.clear()
        val infoWindow = makeStampInfoWindow(context, map, locationOverlay)

        visibleStamps.forEach { stamp ->
            val label = stamp.nombreEn.take(1).uppercase()
            val bitmap = iconBitmaps.getOrPut(label) {
                createCircularIcon(80, Color.parseColor("#FF7043"), label)
            }
            markers += Marker(map).apply {
                icon = BitmapDrawable(context.resources, bitmap)
                title = stamp.nombreEn
                snippet = stamp.direccion
                position = GeoPoint(stamp.lat!!, stamp.lon!!)
                relatedObject = stamp
                this.infoWindow = infoWindow
                setOnMarkerClickListener { marker, _ ->
                    marker.showInfoWindow()
                    true
                }
            }
        }
        map.overlays.addAll(markers)
        map.invalidate()
    }
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
            val latitude: Double = (stamp as? com.mapclover.stampquest.data.model.Stamp)?.lat ?: 0.0
            val longitude: Double = (stamp as? com.mapclover.stampquest.data.model.Stamp)?.lon ?: 0.0
            val distanceText = if (myLocation != null && stamp is com.mapclover.stampquest.data.model.Stamp) {
                val d = myLocation.distanceToAsDouble(GeoPoint(latitude, longitude))
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
private fun createCircularIcon(sizePx: Int, color: Int, letter: String?): Bitmap {
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
    if (letter != null) {
        canvas.drawText(letter, radius, textY, textPaint)
    }

    return bmp
}
