package com.mindmate.data.repository

import com.mindmate.data.local.dao.MessageDao
import com.mindmate.data.local.entity.MessageEntity
import com.mindmate.domain.model.Message
import com.mindmate.domain.model.Sender
import com.mindmate.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(private val messageDao: MessageDao) : ChatRepository {

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    conversationId = entity.conversationId,
                    text = entity.text,
                    sender = Sender.valueOf(entity.sender),
                    timestamp = entity.timestamp,
                    quickActions = entity.quickActions,
                    detectedLanguage = entity.detectedLanguage,
                    safetySignal = entity.safetySignal
                )
            }
        }
    }

    override suspend fun saveMessage(message: Message) {
        val entity = MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            text = message.text,
            sender = message.sender.name,
            timestamp = message.timestamp,
            quickActions = message.quickActions,
            detectedLanguage = message.detectedLanguage,
            safetySignal = message.safetySignal
        )
        messageDao.insertMessage(entity)
    }

    override suspend fun clearConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
    }

    override suspend fun clearAllChatData() {
        messageDao.deleteAllMessages()
    }
}
