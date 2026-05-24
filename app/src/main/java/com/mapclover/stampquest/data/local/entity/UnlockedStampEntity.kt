package com.mapclover.stampquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_stamps")
data class UnlockedStampEntity(
    @PrimaryKey
    val stampId: String,
    val unlockedAt: Long
)