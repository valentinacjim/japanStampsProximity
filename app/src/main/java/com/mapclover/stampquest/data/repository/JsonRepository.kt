package com.mapclover.stampquest.data.repository

import android.content.Context
import com.mapclover.stampquest.data.model.Stamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class JsonRepository(private val context: Context) {

    /**
     * Parses the bundled data once per process.  The file contains thousands of
     * entries, so doing this work on the main thread makes opening the map jank.
     */
    suspend fun loadStamps(): List<Stamp> = withContext(Dispatchers.IO) {
        cachedStamps ?: synchronized(cacheLock) {
            cachedStamps ?: readStamps().also { cachedStamps = it }
        }
    }

    private fun readStamps(): List<Stamp> {
        val jsonString = context.assets
            .open("mapa.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(jsonString)

        val result = mutableListOf<Stamp>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val coords = obj.optJSONObject("coordenadas")

            val stamp = Stamp(
                id = obj.optString("id"),
                nombreJp = obj.optString("nombre_jp"),
                nombreEn = obj.optString("nombre_en"),
                direccion = obj.optString("direccion"),
                url = obj.optString("url"),
                tieneSello = obj.optString("tiene_sello"),
                categoria = obj.optString("categoria"),
                lat = coords?.optDouble("latitud"),
                lon = coords?.optDouble("longitud")
            )

            result.add(stamp)
        }

        return result.filter(::isInSupportedArea)
    }

    fun unlockStamp(stampId: String) {
        // Aquí podrías guardar el estado desbloqueado en SharedPreferences o una base de datos local
        // Por simplicidad, este método no hace nada en esta implementación
    }

    private companion object {
        private val cacheLock = Any()

        @Volatile
        private var cachedStamps: List<Stamp>? = null

        /** The app is intentionally limited to the selected travel destinations. */
        private val supportedAreas = listOf(
            Area(35.52, 35.90, 139.55, 139.95), // Tokio
            Area(34.85, 35.15, 135.55, 135.95), // Kioto
            Area(35.15, 35.30, 138.90, 139.20), // Hakone
            Area(36.05, 36.25, 137.10, 137.35), // Takayama
            Area(35.30, 35.50, 136.65, 136.90), // Gifu
            Area(34.55, 34.85, 135.35, 135.75), // Osaka
            Area(36.55, 37.10, 139.45, 140.05), // Nikko
            Area(35.90, 36.10, 139.65, 139.85), // Kasukabe
            Area(35.25, 35.40, 139.40, 139.60)  // Enoshima
        )

        private fun isInSupportedArea(stamp: Stamp): Boolean {
            val latitude = stamp.lat ?: return false
            val longitude = stamp.lon ?: return false
            return supportedAreas.any { area -> area.contains(latitude, longitude) }
        }
    }

    private data class Area(
        val minLatitude: Double,
        val maxLatitude: Double,
        val minLongitude: Double,
        val maxLongitude: Double
    ) {
        fun contains(latitude: Double, longitude: Double): Boolean =
            latitude in minLatitude..maxLatitude && longitude in minLongitude..maxLongitude
    }
}
