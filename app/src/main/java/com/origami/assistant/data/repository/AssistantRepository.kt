package com.origami.assistant.data.repository

import com.origami.assistant.data.db.dao.AssistantDao
import com.origami.assistant.data.db.dao.SkillDao
import com.origami.assistant.data.db.entity.AssistantEntity
import com.origami.assistant.data.db.entity.SkillEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepository @Inject constructor(
    private val assistantDao: AssistantDao,
    private val skillDao: SkillDao
) {
    fun observeAssistants(): Flow<List<AssistantEntity>> = assistantDao.observeAll()
    suspend fun getAssistant(id: String) = assistantDao.getById(id)
    suspend fun getScheduled() = assistantDao.getScheduled()
    suspend fun saveAssistant(assistant: AssistantEntity) = assistantDao.insert(assistant)
    suspend fun updateAssistant(assistant: AssistantEntity) = assistantDao.update(assistant)
    suspend fun deleteAssistant(id: String) = assistantDao.delete(id)
    suspend fun markRun(id: String) = assistantDao.updateLastRun(id)

    fun observeSkills(): Flow<List<SkillEntity>> = skillDao.observeAll()
    suspend fun getSkill(id: String) = skillDao.getById(id)
    suspend fun getEnabledSkills() = skillDao.getEnabled()
    suspend fun saveSkill(skill: SkillEntity) = skillDao.insert(skill)
    suspend fun setSkillEnabled(id: String, enabled: Boolean) = skillDao.setEnabled(id, enabled)
    suspend fun deleteSkill(id: String) = skillDao.delete(id)
}
