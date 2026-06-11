package com.origami.assistant.ui.modelsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.inference.InferenceBackend
import com.origami.assistant.inference.ModelManager
import com.origami.assistant.inference.ModelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ModelSetupUiState(
    val modelState: ModelState = ModelState.Uninitialized,
    val localPathInput: String = "",
    val selectedBackend: String = "cpu",
    val isModelDownloaded: Boolean = false,
    val downloadProgress: Int = 0,
    val error: String? = null
)

const val GEMMA_KAGGLE_URL = "https://www.kaggle.com/models/google/gemma/tfLite/gemma-2-2b-it-cpu-int8"

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ModelSetupUiState())
    val state: StateFlow<ModelSetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            modelManager.state.collect { ms ->
                _state.update { it.copy(modelState = ms) }
            }
        }
        viewModelScope.launch {
            prefs.modelDownloaded.collect { downloaded ->
                _state.update { it.copy(isModelDownloaded = downloaded) }
            }
        }
        viewModelScope.launch {
            prefs.inferenceBackend.collect { backend ->
                _state.update { it.copy(selectedBackend = backend) }
            }
        }
    }

    fun onLocalPathChange(path: String) = _state.update { it.copy(localPathInput = path) }

    fun onBackendChange(backend: String) {
        _state.update { it.copy(selectedBackend = backend) }
        viewModelScope.launch { prefs.setInferenceBackend(backend) }
    }

    fun loadFromPath() {
        val path = _state.value.localPathInput.trim()
        if (path.isBlank()) return
        if (!File(path).exists()) {
            _state.update { it.copy(error = "File not found: $path") }
            return
        }
        viewModelScope.launch {
            modelManager.loadModel(path)
        }
    }

    fun downloadModel() {
        _state.update {
            it.copy(error = "Auto-download is not available. Please download the model from Kaggle (see instructions below) and enter the file path.")
        }
    }

    fun loadDefault() {
        viewModelScope.launch {
            modelManager.initIfReady()
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
