package com.mapclover.stampquest.data.model

data class Stamp(
    val id: String,
    val nombreJp: String,
    val nombreEn: String,
    val direccion: String,
    val url: String,
    val tieneSello: String,
    val categoria: String,
    val area: String? = null,
    val lat: Double?,
    val lon: Double?
)

