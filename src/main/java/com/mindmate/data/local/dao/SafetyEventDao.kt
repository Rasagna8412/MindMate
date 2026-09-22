package com.mindmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindmate.data.local.entity.SafetyEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyEventDao {
    @Query("SELECT * FROM safety_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SafetyEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SafetyEventEntity)

    @Query("DELETE FROM safety_events")
    suspend fun deleteAllEvents()
}
