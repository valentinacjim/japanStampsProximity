package com.mapclover.stampquest.ui.map

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.DisposableEffect
import com.mapclover.stampquest.data.local.SeenStampsManager
import com.mapclover.stampquest.data.repository.JsonRepository
import com.mapclover.stampquest.domain.service.ProximityService
import com.mapclover.stampquest.domain.usecase.FilterStampsUseCase
import com.mapclover.stampquest.location.EkiProximityLocationTracker
import com.mapclover.stampquest.notification.ProximityNotifier
import com.mapclover.stampquest.ui.filters.StampFilters
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
    val locationTracker = remember { EkiProximityLocationTracker(context) }
    val markerRenderer = remember(context) { MapMarkerRenderer(context) }
    val filterStampsUseCase = remember { FilterStampsUseCase() }
    
    var filters by remember { mutableStateOf(StampFilters()) }
    val visibleStamps = remember(stamps, filters) {
        filterStampsUseCase(stamps, filters, emptySet())
    }
    
    val areas = listOf("Tokyo", "Kyoto", "Hakone", "Takayama", "Gifu", "Osaka", "Nikko", "Kasukabe", "Enoshima")
    val categories = stamps.mapNotNull { it.categoria }.distinct().sorted()

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

    DisposableEffect(stamps) {
        if (stamps.isNotEmpty()) {
            locationTracker.start { location ->
                proximityService.checkProximity(
                    GeoPoint(location.latitude, location.longitude),
                    stamps
                )
            }
        }
        onDispose { locationTracker.stop() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                markerRenderer.setStamps(visibleStamps)
            }
        )

        Surface(
            modifier = Modifier
                .padding(16.dp)
                .align(androidx.compose.ui.Alignment.TopCenter),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Filtros", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Área", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.region == null,
                            onClick = { filters = filters.copy(region = null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(areas) { area ->
                        FilterChip(
                            selected = filters.region == area,
                            onClick = { filters = filters.copy(region = area) },
                            label = { Text(area) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.category == null,
                            onClick = { filters = filters.copy(category = null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = filters.category == category,
                            onClick = { filters = filters.copy(category = category) },
                            label = { Text(category) }
                        )
                    }
                }
            }
        }
    }
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
 * Crea una InfoWindow personalizada que muestra título, dirección, distancia y botón de marcar como visto.
 * Se construye con vistas en código para evitar dependencia de layouts XML.
 */
private fun makeStampInfoWindow(
    context: Context,
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay?
): InfoWindow {
    val seenStampsManager = SeenStampsManager(context)
    
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

    val buttonContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    val markedView = TextView(context).apply {
        text = "✓ Visto"
        setTextColor(Color.GREEN)
        textSize = 12f
        setPadding(8, 8, 8, 8)
    }

    val markButton = Button(context).apply {
        text = "Marcar como visto"
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    container.addView(titleView)
    container.addView(addressView)
    container.addView(distanceView)
    container.addView(buttonContainer)
    buttonContainer.addView(markButton)
    buttonContainer.addView(markedView)

    return object : InfoWindow(container, mapView) {
        override fun onOpen(item: Any?) {
            val marker = item as? Marker ?: return
            val stamp = marker.relatedObject as? com.mapclover.stampquest.data.model.Stamp ?: return

            titleView.text = stamp.nombreEn
            addressView.text = stamp.direccion

            val myLocation = locationOverlay?.myLocation
            val distanceText = if (myLocation != null && stamp.lat != null && stamp.lon != null) {
                val d = myLocation.distanceToAsDouble(GeoPoint(stamp.lat!!, stamp.lon!!))
                "${d.roundToInt()} m"
            } else {
                ""
            }
            distanceView.text = distanceText

            val isSeen = seenStampsManager.isSeen(stamp.id)
            markButton.visibility = if (isSeen) ViewGroup.GONE else ViewGroup.VISIBLE
            markedView.visibility = if (isSeen) ViewGroup.VISIBLE else ViewGroup.GONE

            markButton.setOnClickListener {
                seenStampsManager.markAsSeen(stamp.id)
                markButton.visibility = ViewGroup.GONE
                markedView.visibility = ViewGroup.VISIBLE
            }
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
