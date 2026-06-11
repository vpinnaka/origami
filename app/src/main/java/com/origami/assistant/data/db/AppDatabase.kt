package com.origami.assistant.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.origami.assistant.data.db.dao.*
import com.origami.assistant.data.db.entity.*

@Database(
    entities = [
        MessageEntity::class,
        MemoryEntity::class,
        AssistantEntity::class,
        SkillEntity::class,
        ConversationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun assistantDao(): AssistantDao
    abstract fun skillDao(): SkillDao
    abstract fun conversationDao(): ConversationDao
}
