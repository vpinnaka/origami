package com.origami.assistant.di

import com.origami.assistant.inference.InferenceEngine
import com.origami.assistant.inference.LiteRTEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindInferenceEngine(impl: LiteRTEngine): InferenceEngine
}
