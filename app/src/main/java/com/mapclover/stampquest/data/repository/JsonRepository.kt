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

        return result
    }

    fun unlockStamp(stampId: String) {
        // Aquí podrías guardar el estado desbloqueado en SharedPreferences o una base de datos local
        // Por simplicidad, este método no hace nada en esta implementación
    }

    private companion object {
        private val cacheLock = Any()

        @Volatile
        private var cachedStamps: List<Stamp>? = null
    }
}
