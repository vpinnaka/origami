package com.origami.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    /** Serialized float array (comma-separated) from the embedding model */
    val embedding: String,
    val sourceConversationId: String? = null,
    val tags: String = "",          // comma-separated
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val importance: Float = 0.5f    // 0–1, higher = keep longer
)
