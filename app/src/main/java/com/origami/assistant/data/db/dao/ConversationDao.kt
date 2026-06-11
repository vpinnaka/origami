package com.origami.assistant.data.db.dao

import androidx.room.*
import com.origami.assistant.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE conversations SET updatedAt = :time, totalTokens = :tokens WHERE id = :id")
    suspend fun updateMetadata(id: String, time: Long, tokens: Int)

    @Query("UPDATE conversations SET contextSummary = :summary WHERE id = :id")
    suspend fun updateSummary(id: String, summary: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)
}
