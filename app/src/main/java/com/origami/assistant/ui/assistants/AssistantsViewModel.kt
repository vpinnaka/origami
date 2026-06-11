package com.origami.assistant.ui.assistants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origami.assistant.data.db.entity.AssistantEntity
import com.origami.assistant.data.repository.AssistantRepository
import com.origami.assistant.data.repository.ChatRepository
import com.origami.assistant.scheduler.SchedulerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AssistantsUiState(
    val assistants: List<AssistantEntity> = emptyList(),
    val isCreating: Boolean = false,
    val editingAssistant: AssistantEntity? = null,
    val draftName: String = "",
    val draftDescription: String = "",
    val draftPrompt: String = "",
    val draftSchedule: String = "manual",
    val draftEmoji: String = "🤖"
)

@HiltViewModel
class AssistantsViewModel @Inject constructor(
    private val assistantRepo: AssistantRepository,
    private val chatRepo: ChatRepository,
    private val scheduler: SchedulerManager
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantsUiState())
    val state: StateFlow<AssistantsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            assistantRepo.observeAssistants().collect { list ->
                _state.update { it.copy(assistants = list) }
            }
        }
    }

    fun startCreating() = _state.update {
        it.copy(
            isCreating = true,
            editingAssistant = null,
            draftName = "",
            draftDescription = "",
            draftPrompt = "",
            draftSchedule = "manual",
            draftEmoji = "🤖"
        )
    }

    fun startEditing(assistant: AssistantEntity) = _state.update {
        it.copy(
            isCreating = true,
            editingAssistant = assistant,
            draftName = assistant.name,
            draftDescription = assistant.description,
            draftPrompt = assistant.systemPrompt,
            draftSchedule = assistant.scheduleType,
            draftEmoji = assistant.avatarEmoji
        )
    }

    fun cancelCreating() = _state.update { it.copy(isCreating = false, editingAssistant = null) }

    fun onNameChange(v: String) = _state.update { it.copy(draftName = v) }
    fun onDescChange(v: String) = _state.update { it.copy(draftDescription = v) }
    fun onPromptChange(v: String) = _state.update { it.copy(draftPrompt = v) }
    fun onScheduleChange(v: String) = _state.update { it.copy(draftSchedule = v) }
    fun onEmojiChange(v: String) = _state.update { it.copy(draftEmoji = v) }

    fun save() {
        val s = _state.value
        if (s.draftName.isBlank()) return
        viewModelScope.launch {
            val assistant = (s.editingAssistant ?: AssistantEntity(id = UUID.randomUUID().toString())).copy(
                name = s.draftName,
                description = s.draftDescription,
                systemPrompt = s.draftPrompt.ifBlank {
                    "You are ${s.draftName}, a helpful AI assistant."
                },
                scheduleType = s.draftSchedule,
                avatarEmoji = s.draftEmoji,
                isActive = true
            )
            assistantRepo.saveAssistant(assistant)
            if (assistant.scheduleType != "manual") scheduler.scheduleAssistant(assistant)
            _state.update { it.copy(isCreating = false, editingAssistant = null) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            scheduler.cancelAssistant(id)
            assistantRepo.deleteAssistant(id)
        }
    }

    fun runNow(id: String) {
        scheduler.runNow(id)
    }

    fun startChat(assistantId: String, onChatStarted: (String) -> Unit) {
        viewModelScope.launch {
            val convId = chatRepo.createConversation(assistantId)
            onChatStarted(convId)
        }
    }
}
