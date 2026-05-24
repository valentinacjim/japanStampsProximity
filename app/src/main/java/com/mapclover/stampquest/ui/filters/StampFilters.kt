package com.mapclover.stampquest.ui.filters

data class StampFilters(
    val region: String? = null,
    val category: String? = null,
    val showUnlocked: Boolean = true,
    val maxDistanceKm: Float? = null
)