package com.origami.assistant.data.db.dao

import androidx.room.*
import com.origami.assistant.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun observeMessages(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    suspend fun getMessages(convId: String): List<MessageEntity>

    @Query("SELECT SUM(tokenCount) FROM messages WHERE conversationId = :convId")
    suspend fun getTotalTokens(convId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE conversationId = :convId AND id NOT IN (SELECT id FROM messages WHERE conversationId = :convId ORDER BY timestamp DESC LIMIT :keepLast)")
    suspend fun pruneOldMessages(convId: String, keepLast: Int)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteConversation(convId: String)
}
