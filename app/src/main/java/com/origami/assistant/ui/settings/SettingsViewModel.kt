package com.origami.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.data.repository.MemoryRepository
import com.origami.assistant.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val tavilyKey: String = "",
    val composioKey: String = "",
    val inferenceBackend: String = "cpu",
    val memoryEnabled: Boolean = true,
    val memoryCount: Int = 0,
    val maxContextTokens: Int = 4096,
    val modelPath: String = "",
    val showClearMemoryDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val memoryRepo: MemoryRepository,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.tavilyApiKey.collect { v -> _state.update { it.copy(tavilyKey = v) } }
        }
        viewModelScope.launch {
            prefs.composioApiKey.collect { v -> _state.update { it.copy(composioKey = v) } }
        }
        viewModelScope.launch {
            prefs.inferenceBackend.collect { v -> _state.update { it.copy(inferenceBackend = v) } }
        }
        viewModelScope.launch {
            prefs.memoryEnabled.collect { v -> _state.update { it.copy(memoryEnabled = v) } }
        }
        viewModelScope.launch {
            prefs.maxContextTokens.collect { v -> _state.update { it.copy(maxContextTokens = v) } }
        }
        viewModelScope.launch {
            prefs.modelPath.collect { v -> _state.update { it.copy(modelPath = v) } }
        }
        viewModelScope.launch {
            _state.update { it.copy(memoryCount = memoryRepo.count()) }
        }
    }

    fun setTavilyKey(key: String) {
        _state.update { it.copy(tavilyKey = key) }
        viewModelScope.launch { prefs.setTavilyApiKey(key.trim()) }
    }

    fun setComposioKey(key: String) {
        _state.update { it.copy(composioKey = key) }
        viewModelScope.launch { prefs.setComposioApiKey(key.trim()) }
    }

    fun setBackend(backend: String) {
        viewModelScope.launch { prefs.setInferenceBackend(backend) }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setMemoryEnabled(enabled) }
    }

    fun setMaxTokens(tokens: Int) {
        viewModelScope.launch { prefs.setMaxContextTokens(tokens) }
    }

    fun showClearMemoryDialog() = _state.update { it.copy(showClearMemoryDialog = true) }
    fun dismissClearMemoryDialog() = _state.update { it.copy(showClearMemoryDialog = false) }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepo.deleteAll()
            _state.update { it.copy(memoryCount = 0, showClearMemoryDialog = false) }
        }
    }
}
