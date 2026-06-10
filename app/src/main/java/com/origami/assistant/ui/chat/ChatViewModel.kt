package com.origami.assistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origami.assistant.agent.AgentEvent
import com.origami.assistant.agent.AgentLoop
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.data.repository.ChatRepository
import com.origami.assistant.inference.ModelManager
import com.origami.assistant.inference.ModelState
import com.origami.assistant.ui.chat.model.ChatMessage
import com.origami.assistant.ui.chat.model.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val activeToolName: String? = null,
    val conversationId: String = "",
    val modelState: ModelState = ModelState.Uninitialized,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentLoop: AgentLoop,
    private val chatRepo: ChatRepository,
    private val modelManager: ModelManager,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val DEFAULT_SYSTEM_PROMPT = """
        You are Origami, a helpful, thoughtful personal AI assistant running locally on this device.
        Be concise but thorough. Use tools when they'll produce better answers.
        Speak in a warm, natural tone.
    """.trimIndent()

    init {
        viewModelScope.launch {
            modelManager.state.collect { state ->
                _uiState.update { it.copy(modelState = state) }
            }
        }
        viewModelScope.launch {
            modelManager.initIfReady()
        }
    }

    fun initConversation(conversationId: String?) {
        viewModelScope.launch {
            val id = if (!conversationId.isNullOrBlank()) {
                conversationId
            } else {
                val activeId = prefs.activeConversationId.first()
                if (activeId.isNotBlank()) activeId else chatRepo.createConversation()
            }

            prefs.setActiveConversation(id)
            _uiState.update { it.copy(conversationId = id) }

            // Load history from DB
            chatRepo.observeMessages(id).collect { dbMessages ->
                val uiMessages = dbMessages.map { msg ->
                    ChatMessage(
                        id = msg.id.toString(),
                        role = when (msg.role) {
                            "user" -> MessageRole.USER
                            "tool" -> MessageRole.TOOL
                            else -> MessageRole.ASSISTANT
                        },
                        content = msg.content,
                        toolName = msg.toolName,
                        isToolResult = msg.role == "tool",
                        timestamp = msg.timestamp
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isThinking) return

        val conversationId = _uiState.value.conversationId
        if (conversationId.isBlank()) return

        _uiState.update { it.copy(inputText = "", isThinking = true, error = null) }

        viewModelScope.launch {
            // Optimistically add user message to UI
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.USER,
                content = text
            )
            _uiState.update { state ->
                state.copy(messages = state.messages + userMsg)
            }

            // Start streaming assistant response
            val streamingId = UUID.randomUUID().toString()
            val streamingMsg = ChatMessage(
                id = streamingId,
                role = MessageRole.ASSISTANT,
                content = "",
                isStreaming = true
            )
            _uiState.update { state ->
                state.copy(messages = state.messages + streamingMsg)
            }

            val systemPrompt = DEFAULT_SYSTEM_PROMPT

            agentLoop.run(
                conversationId = conversationId,
                userMessage = text,
                systemPrompt = systemPrompt
            ).collect { event ->
                when (event) {
                    is AgentEvent.Thinking -> {
                        _uiState.update { it.copy(isThinking = true, activeToolName = null) }
                    }
                    is AgentEvent.TokenStream -> {
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.id == streamingId) {
                                    msg.copy(content = msg.content + event.token)
                                } else msg
                            }
                            state.copy(messages = updated)
                        }
                    }
                    is AgentEvent.ToolCalling -> {
                        _uiState.update { it.copy(activeToolName = event.toolName) }
                        val toolMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = MessageRole.TOOL,
                            content = "Calling ${event.toolName}…",
                            toolName = event.toolName,
                            isToolResult = false
                        )
                        _uiState.update { state ->
                            state.copy(messages = state.messages.filterNot { it.id == streamingId } + toolMsg + streamingMsg.copy(content = ""))
                        }
                    }
                    is AgentEvent.ToolResult -> {
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.toolName == event.toolName && !msg.isToolResult) {
                                    msg.copy(
                                        content = event.result.take(300),
                                        isToolResult = true
                                    )
                                } else msg
                            }
                            state.copy(messages = updated, activeToolName = null)
                        }
                    }
                    is AgentEvent.FinalResponse -> {
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.id == streamingId) {
                                    msg.copy(isStreaming = false, content = event.text)
                                } else msg
                            }
                            state.copy(messages = updated, isThinking = false, activeToolName = null)
                        }
                    }
                    is AgentEvent.Error -> {
                        _uiState.update { state ->
                            val updated = state.messages.filterNot { it.id == streamingId }
                            state.copy(
                                messages = updated,
                                isThinking = false,
                                activeToolName = null,
                                error = event.message
                            )
                        }
                    }
                }
            }

            _uiState.update { it.copy(isThinking = false) }
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val id = chatRepo.createConversation()
            prefs.setActiveConversation(id)
            _uiState.update { ChatUiState(conversationId = id, modelState = it.modelState) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
