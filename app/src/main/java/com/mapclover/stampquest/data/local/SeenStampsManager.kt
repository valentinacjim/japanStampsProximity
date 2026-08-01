package com.mapclover.stampquest.data.local

import android.content.Context
import android.content.SharedPreferences

class SeenStampsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("seen_stamps", Context.MODE_PRIVATE)
    
    private companion object {
        private const val SEEN_STAMPS_KEY = "seen_stamps_ids"
    }
    
    fun markAsSeen(stampId: String) {
        val currentIds = getSeenStamps().toMutableSet()
        currentIds.add(stampId)
        prefs.edit().putStringSet(SEEN_STAMPS_KEY, currentIds).apply()
    }
    
    fun isSeen(stampId: String): Boolean {
        return getSeenStamps().contains(stampId)
    }
    
    fun getSeenStamps(): Set<String> {
        return prefs.getStringSet(SEEN_STAMPS_KEY, emptySet()) ?: emptySet()
    }
    
    fun clearSeen(stampId: String) {
        val currentIds = getSeenStamps().toMutableSet()
        currentIds.remove(stampId)
        prefs.edit().putStringSet(SEEN_STAMPS_KEY, currentIds).apply()
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
