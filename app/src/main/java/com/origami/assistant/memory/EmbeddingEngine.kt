package com.origami.assistant.memory

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Lightweight text embedding engine for semantic memory retrieval.
 *
 * Uses a small TF-Lite sentence embedding model. Falls back to TF-IDF-style
 * bag-of-words if the model file is not present — good enough for
 * approximate nearest-neighbour retrieval over personal memories.
 */
@Singleton
class EmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val EMBEDDING_DIM = 384
        private val STOP_WORDS = setOf(
            "a","an","the","is","are","was","were","be","been","being",
            "have","has","had","do","does","did","will","would","could","should",
            "may","might","must","can","i","you","he","she","it","we","they",
            "this","that","these","those","and","or","but","in","on","at","to",
            "for","of","with","by","from","as","into","through","about"
        )
    }

    /**
     * Convert [text] to an embedding vector of dimension [EMBEDDING_DIM].
     * Current implementation: normalised bag-of-words hashing projection.
     * Replace with a proper sentence-transformer TFLite model for production.
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val tokens = tokenize(text)
        val vec = FloatArray(EMBEDDING_DIM)

        tokens.forEach { token ->
            val hash = token.hashCode()
            val idx = ((hash % EMBEDDING_DIM) + EMBEDDING_DIM) % EMBEDDING_DIM
            vec[idx] += 1f
            // Second projection for better separation
            val idx2 = (((hash * 2654435761L.toInt()) % EMBEDDING_DIM) + EMBEDDING_DIM) % EMBEDDING_DIM
            vec[idx2] += 0.5f
        }

        normalise(vec)
    }

    /** Cosine similarity between two unit vectors */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embedding dimension mismatch" }
        return a.zip(b.toList()).sumOf { (x, y) -> (x * y).toDouble() }.toFloat()
    }

    /** Deserialise a stored embedding string back to FloatArray */
    fun deserialise(stored: String): FloatArray =
        stored.split(",").map { it.trim().toFloat() }.toFloatArray()

    /** Serialise a FloatArray for storage */
    fun serialise(vec: FloatArray): String = vec.joinToString(",")

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOP_WORDS }

    private fun normalise(vec: FloatArray): FloatArray {
        val norm = sqrt(vec.map { it * it }.sum())
        if (norm < 1e-8f) return vec
        return FloatArray(vec.size) { vec[it] / norm }
    }
}
