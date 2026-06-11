package com.origami.assistant.data.repository

import com.origami.assistant.data.db.dao.ConversationDao
import com.origami.assistant.data.db.dao.MessageDao
import com.origami.assistant.data.db.entity.ConversationEntity
import com.origami.assistant.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(conversationId)

    suspend fun getMessages(conversationId: String): List<MessageEntity> =
        messageDao.getMessages(conversationId)

    fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAll()

    suspend fun getConversationSummary(conversationId: String): String =
        conversationDao.getById(conversationId)?.contextSummary ?: ""

    suspend fun createConversation(assistantId: String? = null): String {
        val id = UUID.randomUUID().toString()
        conversationDao.insert(
            ConversationEntity(id = id, assistantId = assistantId)
        )
        return id
    }

    suspend fun updateConversationTitle(id: String, title: String) =
        conversationDao.updateTitle(id, title)

    suspend fun updateConversationSummary(id: String, summary: String) =
        conversationDao.updateSummary(id, summary)

    suspend fun pruneOldMessages(conversationId: String, keepLast: Int) =
        messageDao.pruneOldMessages(conversationId, keepLast)

    suspend fun addMessage(message: MessageEntity): Long =
        messageDao.insert(message)

    suspend fun deleteConversation(id: String) {
        conversationDao.delete(id)
        messageDao.deleteConversation(id)
    }
}
