package com.mindmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mindmate.data.local.dao.ConversationDao
import com.mindmate.data.local.dao.JournalDao
import com.mindmate.data.local.dao.MessageDao
import com.mindmate.data.local.dao.MoodDao
import com.mindmate.data.local.dao.SafetyEventDao
import com.mindmate.data.local.entity.ConversationEntity
import com.mindmate.data.local.entity.JournalEntity
import com.mindmate.data.local.entity.MessageEntity
import com.mindmate.data.local.entity.MoodEntity
import com.mindmate.data.local.entity.SafetyEventEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MoodEntity::class,
        JournalEntity::class,
        SafetyEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MindMateDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao
    abstract fun safetyEventDao(): SafetyEventDao

    companion object {
        @Volatile
        private var INSTANCE: MindMateDatabase? = null

        fun getInstance(context: Context): MindMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindMateDatabase::class.java,
                    "mindmate_encrypted.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
