package com.origami.assistant.inference

import com.origami.assistant.agent.model.Tool
import kotlinx.coroutines.flow.Flow

/** Contract for on-device LLM inference */
interface InferenceEngine {
    /** True once the model file is loaded and ready */
    val isReady: Boolean

    /** Load the model from [modelPath]. Must be called before generation. */
    suspend fun initialize(modelPath: String, backend: InferenceBackend = InferenceBackend.CPU)

    /**
     * Generate a streamed response. [prompt] is the full formatted prompt.
     * Returns a Flow of token strings; collect to stream to the UI.
     */
    fun generateStream(prompt: String): Flow<String>

    /**
     * Non-streaming generation used inside the tool-calling loop where
     * we need the full JSON response before parsing tool calls.
     */
    suspend fun generate(prompt: String, maxTokens: Int = 2048): String

    fun close()
}

enum class InferenceBackend { CPU, GPU }
