package com.origami.assistant.data.repository

import com.origami.assistant.data.db.dao.MemoryDao
import com.origami.assistant.data.db.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(private val memoryDao: MemoryDao) {
    fun observeAll(): Flow<List<MemoryEntity>> = memoryDao.observeAll()
    suspend fun getAll(): List<MemoryEntity> = memoryDao.getAll()
    suspend fun delete(id: Long) = memoryDao.delete(id)
    suspend fun deleteAll() = memoryDao.deleteAll()
    suspend fun count(): Int = memoryDao.count()
}
