package com.mapclover.stampquest.ui.filters

data class StampFilters(
    val region: String? = null,
    val category: String? = null,
    val showUnlocked: Boolean = true,
    val seenStatus: SeenStatus? = null,
    val maxDistanceKm: Float? = null
)

enum class SeenStatus {
    FOUND,
    PENDING
}
