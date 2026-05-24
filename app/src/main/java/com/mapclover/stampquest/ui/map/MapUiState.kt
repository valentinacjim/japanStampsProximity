package com.mapclover.stampquest.ui.map

import com.mapclover.stampquest.data.model.Stamp

data class MapUiState(
    val stamps: List<Stamp> = emptyList(),
    val unlockedIds: Set<String> = emptySet(),
    val nearbyStamp: Stamp? = null,
    val selectedCategory: String? = null,
    val showUnlocked: Boolean = true
)