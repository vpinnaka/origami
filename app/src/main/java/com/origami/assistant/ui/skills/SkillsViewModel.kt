package com.origami.assistant.ui.skills

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origami.assistant.data.db.entity.SkillEntity
import com.origami.assistant.data.repository.AssistantRepository
import com.origami.assistant.skills.SkillManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SkillsUiState(
    val skills: List<SkillEntity> = emptyList(),
    val isInstalling: Boolean = false,
    val installError: String? = null
)

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val assistantRepo: AssistantRepository,
    private val skillManager: SkillManager
) : ViewModel() {

    private val _state = MutableStateFlow(SkillsUiState())
    val state: StateFlow<SkillsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            assistantRepo.observeSkills().collect { skills ->
                _state.update { it.copy(skills = skills) }
            }
        }
    }

    fun installFromPath(path: String) {
        val folder = File(path)
        if (!folder.exists() || !folder.isDirectory) {
            _state.update { it.copy(installError = "Invalid folder path: $path") }
            return
        }
        _state.update { it.copy(isInstalling = true, installError = null) }
        viewModelScope.launch {
            skillManager.installFromFolder(folder).fold(
                onSuccess = { _state.update { it.copy(isInstalling = false) } },
                onFailure = { e -> _state.update { it.copy(isInstalling = false, installError = e.message) } }
            )
        }
    }

    fun toggleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { assistantRepo.setSkillEnabled(id, enabled) }
    }

    fun uninstall(id: String) {
        viewModelScope.launch { skillManager.uninstall(id) }
    }

    fun dismissError() = _state.update { it.copy(installError = null) }
}
