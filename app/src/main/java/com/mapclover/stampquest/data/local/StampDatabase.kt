package com.mapclover.stampquest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mapclover.stampquest.data.local.dao.StampDao
import com.mapclover.stampquest.data.local.entity.StampEntity
import com.mapclover.stampquest.data.local.entity.UnlockedStampEntity

@Database(
    entities = [
        StampEntity::class,
        UnlockedStampEntity::class
    ],
    version = 1
)
abstract class StampDatabase : RoomDatabase() {
    abstract fun stampDao(): StampDao
}