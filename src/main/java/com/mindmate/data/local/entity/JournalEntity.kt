package com.mindmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    // Stored with Keystore AES-GCM encryption
    val encryptedWhatHappened: String,
    val encryptedHowIFelt: String,
    val encryptedWhatWasDifficult: String,
    val encryptedWhatHelped: String,
    val moodLevel: Int,
    val tags: List<String>
)
