package com.mindmate.data.repository

import com.mindmate.core.security.EncryptionManager
import com.mindmate.data.local.dao.JournalDao
import com.mindmate.data.local.entity.JournalEntity
import com.mindmate.domain.model.JournalEntry
import com.mindmate.domain.model.MoodScore
import com.mindmate.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JournalRepositoryImpl(private val journalDao: JournalDao) : JournalRepository {

    override fun getAllJournalEntries(): Flow<List<JournalEntry>> {
        return journalDao.getAllJournals().map { entities ->
            entities.map { entity ->
                JournalEntry(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    whatHappened = EncryptionManager.decrypt(entity.encryptedWhatHappened),
                    howIFelt = EncryptionManager.decrypt(entity.encryptedHowIFelt),
                    whatWasDifficult = EncryptionManager.decrypt(entity.encryptedWhatWasDifficult),
                    whatHelped = EncryptionManager.decrypt(entity.encryptedWhatHelped),
                    mood = MoodScore.fromLevel(entity.moodLevel),
                    tags = entity.tags
                )
            }
        }
    }

    override suspend fun saveJournalEntry(entry: JournalEntry) {
        val entity = JournalEntity(
            id = entry.id,
            timestamp = entry.timestamp,
            encryptedWhatHappened = EncryptionManager.encrypt(entry.whatHappened),
            encryptedHowIFelt = EncryptionManager.encrypt(entry.howIFelt),
            encryptedWhatWasDifficult = EncryptionManager.encrypt(entry.whatWasDifficult),
            encryptedWhatHelped = EncryptionManager.encrypt(entry.whatHelped),
            moodLevel = entry.mood.level,
            tags = entry.tags
        )
        journalDao.insertJournal(entity)
    }

    override suspend fun deleteJournalEntry(id: String) {
        journalDao.deleteJournal(id)
    }

    override suspend fun clearAllJournalData() {
        journalDao.deleteAllJournals()
    }
}
