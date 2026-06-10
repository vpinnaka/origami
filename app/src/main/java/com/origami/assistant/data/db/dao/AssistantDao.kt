package com.origami.assistant.data.db.dao

import androidx.room.*
import com.origami.assistant.data.db.entity.AssistantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    @Query("SELECT * FROM assistants ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AssistantEntity>>

    @Query("SELECT * FROM assistants WHERE id = :id")
    suspend fun getById(id: String): AssistantEntity?

    @Query("SELECT * FROM assistants WHERE scheduleType != 'manual' AND isActive = 1")
    suspend fun getScheduled(): List<AssistantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assistant: AssistantEntity)

    @Update
    suspend fun update(assistant: AssistantEntity)

    @Query("DELETE FROM assistants WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE assistants SET lastRunAt = :time WHERE id = :id")
    suspend fun updateLastRun(id: String, time: Long = System.currentTimeMillis())
}
