package com.origami.assistant.agent

import com.origami.assistant.agent.model.*
import com.origami.assistant.data.db.entity.MessageEntity
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.data.repository.ChatRepository
import com.origami.assistant.inference.InferenceEngine
import com.origami.assistant.inference.PromptFormatter
import com.origami.assistant.memory.ContextManager
import com.origami.assistant.memory.MemoryManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class AgentEvent {
    data class TokenStream(val token: String) : AgentEvent()
    data class ToolCalling(val toolName: String) : AgentEvent()
    data class ToolResult(val toolName: String, val result: String, val isError: Boolean) : AgentEvent()
    data class FinalResponse(val text: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    object Thinking : AgentEvent()
}

/**
 * One-tool-at-a-time agent loop.
 *
 * Pattern: generate → parse tool call → execute → inject result → repeat
 * Max [maxIterations] iterations to prevent runaway chains.
 */
@Singleton
class AgentLoop @Inject constructor(
    private val engine: InferenceEngine,
    private val toolRegistry: ToolRegistry,
    private val chatRepo: ChatRepository,
    private val memoryManager: MemoryManager,
    private val contextManager: ContextManager,
    private val prefs: AppPreferences
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    companion object {
        const val MAX_ITERATIONS = 8
    }

    /**
     * Run the agent loop for a user message in [conversationId].
     * Emits [AgentEvent]s as work progresses.
     */
    fun run(
        conversationId: String,
        userMessage: String,
        systemPrompt: String,
        enabledTools: Set<String> = setOf("web_search", "calculate", "calendar")
    ): Flow<AgentEvent> = flow {
        if (!engine.isReady) {
            emit(AgentEvent.Error("Model not loaded. Go to Settings to set up the model."))
            return@flow
        }

        // Save user message
        chatRepo.addMessage(MessageEntity(
            conversationId = conversationId,
            role = "user",
            content = userMessage
        ))

        // Load conversation context
        val history = contextManager.getContext(conversationId)
        val memories = memoryManager.retrieveRelevant(userMessage, topK = 5)
        val memorySummary = memories.joinToString("\n") { "- ${it.content}" }
        val contextSummary = chatRepo.getConversationSummary(conversationId)

        val tools = toolRegistry.getDefinitions(enabledTools)
        val assistantMessageParts = mutableListOf<String>()

        var iterHistory = history.toMutableList()
        var iteration = 0

        while (iteration < MAX_ITERATIONS) {
            iteration++
            emit(AgentEvent.Thinking)

            val prompt = PromptFormatter.buildChatPrompt(
                systemPrompt = systemPrompt,
                messages = iterHistory,
                tools = tools,
                memories = memorySummary,
                contextSummary = contextSummary
            )

            val response = engine.generate(prompt)
            Timber.d("Agent iter $iteration response: ${response.take(200)}")

            val toolCallJson = PromptFormatter.extractToolCall(response)

            if (toolCallJson != null) {
                val textPart = PromptFormatter.stripToolCall(response)
                if (textPart.isNotBlank()) {
                    assistantMessageParts.add(textPart)
                    emit(AgentEvent.TokenStream(textPart))
                }

                val toolCall = parseToolCall(toolCallJson)
                if (toolCall == null) {
                    emit(AgentEvent.Error("Failed to parse tool call"))
                    break
                }

                emit(AgentEvent.ToolCalling(toolCall.name))
                val toolResult = toolRegistry.execute(toolCall)
                emit(AgentEvent.ToolResult(toolResult.toolName, toolResult.result, toolResult.isError))

                // Save assistant turn with tool call indicator
                val assistantMsg = MessageEntity(
                    conversationId = conversationId,
                    role = "assistant",
                    content = response,
                    toolCallId = toolCall.id
                )
                iterHistory.add(assistantMsg)

                // Save tool result turn
                val toolMsg = MessageEntity(
                    conversationId = conversationId,
                    role = "tool",
                    content = toolResult.result,
                    toolName = toolResult.toolName,
                    toolCallId = toolCall.id
                )
                iterHistory.add(toolMsg)

                chatRepo.addMessage(assistantMsg)
                chatRepo.addMessage(toolMsg)

            } else {
                // No tool call — final response
                val finalText = response.trim()
                assistantMessageParts.add(finalText)

                val finalMsg = MessageEntity(
                    conversationId = conversationId,
                    role = "assistant",
                    content = finalText
                )
                chatRepo.addMessage(finalMsg)

                // Stream tokens for the UI
                finalText.split(" ").forEach { word ->
                    emit(AgentEvent.TokenStream("$word "))
                }

                emit(AgentEvent.FinalResponse(finalText))

                // Auto-manage context when token count grows high
                contextManager.compressIfNeeded(conversationId)

                // Extract and store memory from this exchange
                memoryManager.extractAndStore(conversationId, userMessage, finalText)

                break
            }
        }

        if (iteration >= MAX_ITERATIONS) {
            emit(AgentEvent.Error("Agent reached maximum iterations ($MAX_ITERATIONS). Try a simpler request."))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseToolCall(json: String): ToolCall? = try {
        val adapter = moshi.adapter(Map::class.java)
        val outer = adapter.fromJson(json) as? Map<String, Any> ?: return null
        val toolCallObj = outer["tool_call"] as? Map<String, Any> ?: return null
        val name = toolCallObj["name"] as? String ?: return null
        val parameters = toolCallObj["parameters"] as? Map<String, Any> ?: emptyMap()
        ToolCall(id = UUID.randomUUID().toString(), name = name, parameters = parameters)
    } catch (e: Exception) {
        Timber.e(e, "Failed to parse tool call JSON: $json")
        null
    }
}
