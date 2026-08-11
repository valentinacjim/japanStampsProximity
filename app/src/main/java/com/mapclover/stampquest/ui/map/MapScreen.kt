package com.mapclover.stampquest.ui.map

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.compose.runtime.DisposableEffect
import com.mapclover.stampquest.data.local.SeenStampsManager
import com.mapclover.stampquest.data.repository.JsonRepository
import com.mapclover.stampquest.domain.usecase.FilterStampsUseCase
import com.mapclover.stampquest.location.ProximityTrackingPreferences
import com.mapclover.stampquest.location.ProximityTrackingService
import com.mapclover.stampquest.ui.filters.StampFilters
import com.mapclover.stampquest.ui.filters.SeenStatus
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    onCollectionClick: () -> Unit,
    focusStampId: String? = null
) {
    val context = LocalContext.current
    val repository = remember { JsonRepository(context) }
    val stamps = androidx.compose.runtime.produceState(
        initialValue = emptyList<com.mapclover.stampquest.data.model.Stamp>(),
        repository
    ) {
        value = repository.loadStamps()
    }.value
    val trackingPreferences = remember { ProximityTrackingPreferences(context) }
    var trackingEnabled by remember { mutableStateOf(trackingPreferences.isEnabled) }
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val seenStampsManager = remember { SeenStampsManager(context) }
    var filters by remember { mutableStateOf(StampFilters()) }
    var seenStampIds by remember { mutableStateOf(seenStampsManager.getSeenStamps()) }
    val markerRenderer = remember(context) {
        MapMarkerRenderer(context) {
            seenStampIds = seenStampsManager.getSeenStamps()
        }
    }
    val filterStampsUseCase = remember { FilterStampsUseCase() }
    val visibleStamps = remember(stamps, filters, seenStampIds) {
        filterStampsUseCase(stamps, filters, seenStampIds)
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

    LaunchedEffect(stamps, focusStampId) {
        val stamp = stamps.firstOrNull { it.id == focusStampId } ?: return@LaunchedEffect
        mapView.post { markerRenderer.focusStamp(stamp) }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Eki Stamps", style = MaterialTheme.typography.titleMedium)
                        Text("${visibleStamps.size} en el mapa", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onCollectionClick) {
                        Text("Colección (${seenStampIds.size})")
                    }
                }
                TextButton(
                    enabled = hasLocationPermission,
                    onClick = {
                        trackingEnabled = !trackingEnabled
                        trackingPreferences.isEnabled = trackingEnabled
                        if (trackingEnabled) ProximityTrackingService.start(context)
                        else ProximityTrackingService.stop(context)
                    }
                ) {
                    Text(if (trackingEnabled) "Desactivar alertas" else "Activar alertas cercanas")
                }
                if (!hasLocationPermission) {
                    Text("Concede permiso de ubicación para activar las alertas.", style = MaterialTheme.typography.bodySmall)
                }
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
                Spacer(modifier = Modifier.height(8.dp))
                Text("Estado", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.seenStatus == null,
                            onClick = { filters = filters.copy(seenStatus = null) },
                            label = { Text("Todos") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.seenStatus == SeenStatus.FOUND,
                            onClick = { filters = filters.copy(seenStatus = SeenStatus.FOUND) },
                            label = { Text("Encontrados") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.seenStatus == SeenStatus.PENDING,
                            onClick = { filters = filters.copy(seenStatus = SeenStatus.PENDING) },
                            label = { Text("Pendientes") }
                        )
                    }
                }
            }
        }
    }
}

/** Keeps map overlays outside Compose state and only creates markers in the viewport. */
private class MapMarkerRenderer(
    private val context: Context,
    private val onStampSeen: () -> Unit
) : MapListener {
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

    fun focusStamp(stamp: com.mapclover.stampquest.data.model.Stamp) {
        val latitude = stamp.lat ?: return
        val longitude = stamp.lon ?: return
        if (!::map.isInitialized) return

        map.controller.setZoom(16.0)
        map.controller.animateTo(GeoPoint(latitude, longitude))
        lastViewportKey = null
        renderHandler.removeCallbacks(deferredRender)
        renderHandler.postDelayed({
            renderVisibleMarkers()
            markers.firstOrNull { marker ->
                (marker.relatedObject as? com.mapclover.stampquest.data.model.Stamp)?.id == stamp.id
            }?.showInfoWindow()
        }, 350)
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
        val infoWindow = makeStampInfoWindow(context, map, locationOverlay, onStampSeen)

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
    locationOverlay: MyLocationNewOverlay?,
    onStampSeen: () -> Unit
): InfoWindow {
    val seenStampsManager = SeenStampsManager(context)
    
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 24, 32, 24)
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 28f
            setStroke(2, Color.parseColor("#E5E7EB"))
        }
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        elevation = 14f
    }

    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val titleView = TextView(context).apply {
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.BLACK)
        textSize = 16f
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    val minimizeButton = Button(context).apply {
        text = "−"
        textSize = 18f
        minWidth = 0
        minimumWidth = 0
        setPadding(16, 0, 16, 0)
    }

    val addressView = TextView(context).apply {
        setTextColor(Color.DKGRAY)
        textSize = 14f
    }

    val distanceView = TextView(context).apply {
        setTextColor(Color.GRAY)
        textSize = 12f
        gravity = Gravity.START
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
        text = "Marcar encontrado"
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    header.addView(titleView)
    header.addView(minimizeButton)
    container.addView(header)
    container.addView(addressView)
    container.addView(distanceView)
    container.addView(buttonContainer)
    buttonContainer.addView(markButton)
    buttonContainer.addView(markedView)

    return object : InfoWindow(container, mapView) {
        private var minimized = false

        private fun updateMinimized() {
            val detailVisibility = if (minimized) View.GONE else View.VISIBLE
            addressView.visibility = detailVisibility
            distanceView.visibility = detailVisibility
            buttonContainer.visibility = detailVisibility
            minimizeButton.text = if (minimized) "+" else "−"
        }

        override fun onOpen(item: Any?) {
            val marker = item as? Marker ?: return
            val stamp = marker.relatedObject as? com.mapclover.stampquest.data.model.Stamp ?: return

            titleView.text = stamp.nombreEn
            addressView.text = stamp.direccion
            minimized = false
            updateMinimized()

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
            markedView.text = "✓ Encontrado"

            minimizeButton.setOnClickListener {
                minimized = !minimized
                updateMinimized()
            }

            markButton.setOnClickListener {
                seenStampsManager.markAsSeen(stamp.id)
                markButton.visibility = ViewGroup.GONE
                markedView.visibility = ViewGroup.VISIBLE
                onStampSeen()
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
        this.color = Color.WHITE
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
