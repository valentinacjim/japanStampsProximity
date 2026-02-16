package com.mapclover.stampquest.data.model

data class Stamp(
    val id: String,
    val name: String,
    val englishName: String?,
    val address: String?,
    val url: String?,
    val hasStamp: Boolean,
    val latitude: Double,
    val longitude: Double
)

