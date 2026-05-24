package com.mapclover.stampquest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapclover.stampquest.data.local.entity.StampEntity
import com.mapclover.stampquest.data.local.entity.UnlockedStampEntity

@Dao
interface StampDao {

    @Query("SELECT * FROM stamps")
    suspend fun getAllStamps(): List<StampEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStamps(stamps: List<StampEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockStamp(stamp: UnlockedStampEntity)

    @Query("SELECT stampId FROM unlocked_stamps")
    suspend fun getUnlockedIds(): List<String>
}