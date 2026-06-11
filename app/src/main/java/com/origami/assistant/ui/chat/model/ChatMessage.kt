package com.origami.assistant.ui.chat.model

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false,
    val toolName: String? = null,
    val isToolResult: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole { USER, ASSISTANT, TOOL }
