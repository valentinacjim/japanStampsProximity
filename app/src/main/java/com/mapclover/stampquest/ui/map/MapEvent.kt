package com.mapclover.stampquest.ui.map

sealed class MapEvent {
    data object VibrateUnlock : MapEvent()
}