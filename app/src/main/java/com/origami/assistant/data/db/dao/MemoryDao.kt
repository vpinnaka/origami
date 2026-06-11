package com.origami.assistant.data.db.dao

import androidx.room.*
import com.origami.assistant.data.db.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY importance DESC, lastAccessedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY importance DESC, lastAccessedAt DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY importance DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessedAt = :now WHERE id = :id")
    suspend fun recordAccess(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int
}
