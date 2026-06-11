package com.origami.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,           // "user" | "assistant" | "tool"
    val content: String,
    val toolName: String? = null,
    val toolCallId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0
)
