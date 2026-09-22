package com.mindmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_events")
data class SafetyEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val safetyLevel: String,
    val reason: String,
    val matchedPatterns: List<String>
)
