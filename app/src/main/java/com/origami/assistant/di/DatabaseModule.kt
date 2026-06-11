package com.origami.assistant.di

import android.content.Context
import androidx.room.Room
import com.origami.assistant.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "origami.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideMessageDao(db: AppDatabase) = db.messageDao()
    @Provides fun provideMemoryDao(db: AppDatabase) = db.memoryDao()
    @Provides fun provideAssistantDao(db: AppDatabase) = db.assistantDao()
    @Provides fun provideSkillDao(db: AppDatabase) = db.skillDao()
    @Provides fun provideConversationDao(db: AppDatabase) = db.conversationDao()
}
