package com.mapclover.stampquest.data.repository

import android.content.Context
import androidx.compose.ui.Modifier
import com.mapclover.stampquest.R
import com.mapclover.stampquest.data.model.Stamp
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory


class KmlRepository(private val context: Context) {

    fun loadStamps(): List<Stamp> {
        val stamps = mutableListOf<Stamp>()

        val inputStream = context.resources.openRawResource(R.raw.mapa)
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)

        val placemarks = document.getElementsByTagName("Placemark")

        for (i in 0 until placemarks.length) {
            val placemark = placemarks.item(i) as Element

            val name = placemark.getElementsByTagName("name")
                .item(0).textContent

            val coordinates = placemark
                .getElementsByTagName("coordinates")
                .item(0)
                .textContent.trim()

            val parts = coordinates.split(",")
            val longitude = parts[0].toDouble()
            val latitude = parts[1].toDouble()

            // ExtendedData
            val dataNodes = placemark.getElementsByTagName("Data")

            var id = ""
            var englishName: String? = null
            var address: String? = null
            var url: String? = null
            var hasStamp = false

            for (j in 0 until dataNodes.length) {
                val data = dataNodes.item(j) as Element
                val key = data.getAttribute("name")
                val value = data
                    .getElementsByTagName("value")
                    .item(0)
                    ?.textContent ?: ""

                when (key) {
                    "ID" -> id = value
                    "EN" -> englishName = value
                    "住所" -> address = value
                    "URL" -> url = value
                    "スタンプ設置" -> hasStamp = value.contains("設置あり")
                }
            }

            stamps.add(
                Stamp(
                    id,
                    name,
                    englishName,
                    address,
                    url,
                    hasStamp,
                    latitude,
                    longitude,
                    region = "Unknown",
                    category = "Unknown"
                )
            )
        }

        return stamps
    }

    fun unlockStamp(id: String) {}
}
