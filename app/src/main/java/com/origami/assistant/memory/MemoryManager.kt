package com.origami.assistant.memory

import com.origami.assistant.data.db.dao.MemoryDao
import com.origami.assistant.data.db.entity.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class MemoryMatch(
    val id: Long,
    val content: String,
    val similarity: Float,
    val importance: Float
)

@Singleton
class MemoryManager @Inject constructor(
    private val memoryDao: MemoryDao,
    private val embedding: EmbeddingEngine
) {
    /**
     * Retrieve the [topK] most relevant memories for [query] using cosine similarity.
     * Falls back to recency if embeddings are unavailable.
     */
    suspend fun retrieveRelevant(query: String, topK: Int = 5): List<MemoryMatch> =
        withContext(Dispatchers.Default) {
            val all = memoryDao.getAll()
            if (all.isEmpty()) return@withContext emptyList()

            val queryVec = embedding.embed(query)
            all.map { mem ->
                val memVec = try {
                    embedding.deserialise(mem.embedding)
                } catch (e: Exception) {
                    return@map MemoryMatch(mem.id, mem.content, 0f, mem.importance)
                }
                val sim = embedding.cosineSimilarity(queryVec, memVec)
                MemoryMatch(mem.id, mem.content, sim, mem.importance)
            }
                .sortedByDescending { it.similarity * 0.7f + it.importance * 0.3f }
                .take(topK)
                .also { matches ->
                    matches.forEach { memoryDao.recordAccess(it.id) }
                }
        }

    /**
     * Store a new memory from a completed conversation exchange.
     * Extracts facts worth remembering using keyword heuristics.
     */
    suspend fun extractAndStore(
        conversationId: String,
        userMessage: String,
        assistantReply: String
    ) = withContext(Dispatchers.IO) {
        val facts = extractFacts(userMessage, assistantReply)
        facts.forEach { fact ->
            val vec = embedding.embed(fact)
            memoryDao.insert(
                MemoryEntity(
                    content = fact,
                    embedding = embedding.serialise(vec),
                    sourceConversationId = conversationId,
                    importance = scoreImportance(fact)
                )
            )
        }
        Timber.d("Stored ${facts.size} memories from conversation")
    }

    suspend fun addExplicit(content: String, importance: Float = 0.8f) {
        val vec = embedding.embed(content)
        memoryDao.insert(
            MemoryEntity(
                content = content,
                embedding = embedding.serialise(vec),
                importance = importance
            )
        )
    }

    suspend fun delete(id: Long) = memoryDao.delete(id)
    suspend fun clearAll() = memoryDao.deleteAll()

    private fun extractFacts(user: String, assistant: String): List<String> {
        val facts = mutableListOf<String>()

        // User stated preferences / personal information
        val prefPatterns = listOf(
            Regex("(I (?:like|love|prefer|hate|dislike|enjoy|use|work|am|have) .+)", RegexOption.IGNORE_CASE),
            Regex("(My (?:name|job|work|company|location|city|goal|project) (?:is|are) .+)", RegexOption.IGNORE_CASE),
            Regex("(I'?m (?:a|an|the) .+)", RegexOption.IGNORE_CASE),
            Regex("(remind me .+)", RegexOption.IGNORE_CASE)
        )
        prefPatterns.forEach { pattern ->
            pattern.find(user)?.groupValues?.getOrNull(1)?.let { match ->
                if (match.length in 10..200) facts.add(match.trim())
            }
        }

        // Key facts from assistant reply (sentences with numbers, names, dates)
        val factPatterns = listOf(
            Regex("""[A-Z][^.!?]{20,100}[\d%$€£][^.!?]{0,50}[.!?]"""),
            Regex("""[A-Z][^.!?]{10,100}(?:is|are|was|were)[^.!?]{5,80}[.!?]""")
        )
        factPatterns.forEach { pattern ->
            pattern.findAll(assistant).take(2).forEach { match ->
                facts.add(match.value.trim())
            }
        }

        return facts.distinctBy { it.lowercase().take(40) }.take(5)
    }

    private fun scoreImportance(fact: String): Float {
        var score = 0.5f
        if (fact.contains("I ", ignoreCase = true)) score += 0.2f
        if (fact.contains("my ", ignoreCase = true)) score += 0.1f
        if (fact.length > 50) score += 0.1f
        return score.coerceIn(0f, 1f)
    }
}
