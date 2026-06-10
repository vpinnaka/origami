package com.origami.assistant.memory

import com.origami.assistant.data.db.entity.MessageEntity
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the sliding context window for conversations.
 *
 * When the conversation token count exceeds [maxTokens], older messages are
 * summarized (heuristically) and replaced with a summary entry so the
 * prompt stays within the model's context window.
 */
@Singleton
class ContextManager @Inject constructor(
    private val chatRepo: ChatRepository,
    private val prefs: AppPreferences
) {
    companion object {
        private const val APPROX_CHARS_PER_TOKEN = 4
        private const val KEEP_RECENT_MESSAGES = 10
        private const val COMPRESSION_THRESHOLD = 0.85f
    }

    /** Returns conversation history trimmed to fit within the context window */
    suspend fun getContext(conversationId: String): List<MessageEntity> {
        val maxTokens = prefs.maxContextTokens.first()
        val messages = chatRepo.getMessages(conversationId)

        var totalChars = messages.sumOf { it.content.length }
        val totalTokensApprox = totalChars / APPROX_CHARS_PER_TOKEN

        if (totalTokensApprox <= maxTokens) return messages

        // Return only recent messages + system summary
        return messages.takeLast(KEEP_RECENT_MESSAGES)
    }

    /** Compress context if the conversation is getting too long */
    suspend fun compressIfNeeded(conversationId: String) {
        val maxTokens = prefs.maxContextTokens.first()
        val messages = chatRepo.getMessages(conversationId)
        val totalTokensApprox = messages.sumOf { it.content.length } / APPROX_CHARS_PER_TOKEN

        if (totalTokensApprox < maxTokens * COMPRESSION_THRESHOLD) return

        Timber.d("Compressing context for $conversationId (${totalTokensApprox} tokens)")

        // Build a heuristic summary of older messages
        val olderMessages = messages.dropLast(KEEP_RECENT_MESSAGES)
        val summary = buildSummary(olderMessages)

        chatRepo.updateConversationSummary(conversationId, summary)
        chatRepo.pruneOldMessages(conversationId, keepLast = KEEP_RECENT_MESSAGES)
    }

    private fun buildSummary(messages: List<MessageEntity>): String = buildString {
        appendLine("Summary of earlier conversation:")
        messages.filter { it.role != "tool" }
            .chunked(2)
            .take(20)
            .forEach { pair ->
                val user = pair.firstOrNull { it.role == "user" }?.content?.take(100)
                val asst = pair.firstOrNull { it.role == "assistant" }?.content?.take(150)
                if (user != null) appendLine("User: $user…")
                if (asst != null) appendLine("Assistant: $asst…")
            }
    }
}
