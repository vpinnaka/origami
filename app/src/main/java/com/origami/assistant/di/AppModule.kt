package com.origami.assistant.di

import android.content.Context
import com.origami.assistant.agent.AgentLoop
import com.origami.assistant.agent.ToolRegistry
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.data.repository.AssistantRepository
import com.origami.assistant.data.repository.ChatRepository
import com.origami.assistant.data.repository.MemoryRepository
import com.origami.assistant.inference.ModelManager
import com.origami.assistant.memory.ContextManager
import com.origami.assistant.memory.MemoryManager
import com.origami.assistant.skills.SkillExecutor
import com.origami.assistant.skills.SkillManager
import com.origami.assistant.terminal.TerminalSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTerminalSession(@ApplicationContext ctx: Context): TerminalSession =
        TerminalSession(ctx)

    @Provides
    @Singleton
    fun provideSkillExecutor(terminalSession: TerminalSession): SkillExecutor =
        SkillExecutor(terminalSession)

    @Provides
    @Singleton
    fun provideContextManager(
        chatRepo: ChatRepository,
        prefs: AppPreferences
    ): ContextManager = ContextManager(chatRepo, prefs)
}
