package com.swasthai.app.di

import android.content.Context
import com.swasthai.app.ai.engine.AIEngineManager
import com.swasthai.app.ai.engine.ClinicalReasoningEngine
import com.swasthai.app.ai.engine.ImageClassifier
import com.swasthai.app.ai.engine.ModelLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Edge AI dependencies.
 *
 * Provides the ModelLoader for TFLite model management and
 * the AIEngineManager that orchestrates multi-modal AI inference.
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideModelLoader(
        @ApplicationContext context: Context
    ): ModelLoader = ModelLoader(context)

    @Provides
    @Singleton
    fun provideImageClassifier(
        @ApplicationContext context: Context
    ): ImageClassifier = ImageClassifier(context)

    @Provides
    @Singleton
    fun provideClinicalReasoningEngine(): ClinicalReasoningEngine = ClinicalReasoningEngine()

    @Provides
    @Singleton
    fun provideAIEngineManager(
        modelLoader: ModelLoader,
        imageClassifier: ImageClassifier,
        reasoningEngine: ClinicalReasoningEngine
    ): AIEngineManager = AIEngineManager(modelLoader, imageClassifier, reasoningEngine)
}
