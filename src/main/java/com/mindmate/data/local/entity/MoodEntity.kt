package com.mindmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val scoreLevel: Int,
    val scoreLabel: String,
    val note: String,
    val tags: List<String>
)
