package com.origami.assistant.inference

import android.content.Context
import com.origami.assistant.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class ModelState {
    object Uninitialized : ModelState()
    data class Downloading(val progress: Int) : ModelState()
    object Loading : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: LiteRTEngine,
    private val prefs: AppPreferences
) {
    private val _state = MutableStateFlow<ModelState>(ModelState.Uninitialized)
    val state: StateFlow<ModelState> = _state

    /** Models directory inside app's private storage */
    val modelsDir: File get() = File(context.filesDir, "models").also { it.mkdirs() }

    val defaultModelPath: String get() = File(modelsDir, "gemma4-e2b.task").absolutePath

    suspend fun initIfReady() {
        val path = prefs.modelPath.first()
        if (path.isNotBlank() && File(path).exists()) {
            loadModel(path)
        } else {
            val default = defaultModelPath
            if (File(default).exists()) {
                prefs.setModelPath(default)
                loadModel(default)
            }
        }
    }

    suspend fun loadModel(path: String, backend: InferenceBackend = InferenceBackend.CPU) {
        _state.value = ModelState.Loading
        try {
            val backendPref = prefs.inferenceBackend.first()
            val resolvedBackend = if (backendPref == "gpu") InferenceBackend.GPU else InferenceBackend.CPU
            engine.initialize(path, resolvedBackend)
            prefs.setModelPath(path)
            _state.value = ModelState.Ready
        } catch (e: Exception) {
            Timber.e(e, "Model load failed")
            _state.value = ModelState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Download Gemma 4 E2B from Hugging Face / Google's CDN.
     * Progress is reported via [onProgress] (0–100).
     */
    suspend fun downloadModel(
        url: String,
        onProgress: (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val dest = File(modelsDir, "gemma4-e2b.task")
        _state.value = ModelState.Downloading(0)
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connect()
            val total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = ((downloaded.toFloat() / total) * 100).toInt()
                            _state.value = ModelState.Downloading(pct)
                            onProgress(pct)
                        }
                    }
                }
            }
            prefs.setModelDownloaded(true)
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            dest.delete()
            _state.value = ModelState.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    fun isReady() = engine.isReady
}
