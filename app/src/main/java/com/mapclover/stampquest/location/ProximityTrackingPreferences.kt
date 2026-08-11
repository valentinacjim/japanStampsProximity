package com.mapclover.stampquest.location

import android.content.Context

class ProximityTrackingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("proximity_tracking", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = preferences.getBoolean(ENABLED_KEY, false)
        set(value) = preferences.edit().putBoolean(ENABLED_KEY, value).apply()

    private companion object {
        const val ENABLED_KEY = "enabled"
    }
}
