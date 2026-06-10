package com.origami.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistants")
data class AssistantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val avatarEmoji: String = "🤖",
    /** Comma-separated skill IDs enabled for this assistant */
    val enabledSkillIds: String = "",
    /** Comma-separated tool names enabled for this assistant */
    val enabledTools: String = "web_search",
    val scheduleType: String = "manual",   // "manual" | "daily" | "weekly"
    val scheduleTimeHour: Int = 8,
    val scheduleTimeMinute: Int = 0,
    val scheduleDayOfWeek: Int = 2,        // 1=Mon … 7=Sun
    val isActive: Boolean = true,
    val lastRunAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
