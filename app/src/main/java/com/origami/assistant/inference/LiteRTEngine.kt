package com.origami.assistant.inference

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * LiteRT inference engine via the MediaPipe LLM Inference API.
 *
 * Gemma 4 E2B runs at ~2.6 GB. CPU backend is the safe default.
 * GPU requires ~3GB VRAM and will SIGSEGV on devices with <12GB RAM.
 */
@Singleton
class LiteRTEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : InferenceEngine {

    private var llm: LlmInference? = null
    private val _ready = AtomicBoolean(false)

    override val isReady: Boolean get() = _ready.get()

    override suspend fun initialize(modelPath: String, backend: InferenceBackend) {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Initializing LiteRT engine: path=$modelPath backend=$backend")

                val mediaPipeBackend = when (backend) {
                    InferenceBackend.GPU -> LlmInference.Backend.GPU
                    InferenceBackend.CPU -> LlmInference.Backend.CPU
                }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(4096)
                    .setMaxTopK(40)
                    .setPreferredBackend(mediaPipeBackend)
                    .build()

                llm?.close()
                llm = LlmInference.createFromOptions(context, options)
                _ready.set(true)
                Timber.i("LiteRT engine ready")
            } catch (e: Exception) {
                _ready.set(false)
                Timber.e(e, "Failed to initialize LiteRT engine")
                throw e
            }
        }
    }

    override fun generateStream(prompt: String): Flow<String> = callbackFlow {
        val engine = requireReady()
        try {
            engine.generateResponseAsync(prompt) { partial, done ->
                if (partial != null) trySend(partial)
                if (done) close()
            }
        } catch (e: Exception) {
            close(e)
        }
        awaitClose()
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(prompt: String, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val engine = requireReady()
            suspendCancellableCoroutine { cont ->
                val sb = StringBuilder()
                try {
                    engine.generateResponseAsync(prompt) { partial, done ->
                        if (partial != null) sb.append(partial)
                        if (done) cont.resume(sb.toString())
                    }
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        }

    override fun close() {
        llm?.close()
        llm = null
        _ready.set(false)
    }

    private fun requireReady(): LlmInference =
        llm ?: error("LiteRTEngine not initialized — call initialize() first")
}
