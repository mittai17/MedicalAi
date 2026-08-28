package com.swasthai.app.di

import android.content.Context
import com.swasthai.app.ai.engine.AIEngineManager
import com.swasthai.app.ai.engine.ClinicalReasoningEngine
import com.swasthai.app.ai.engine.DeviceCapabilityGate
import com.swasthai.app.ai.engine.GemmaFallbackClient
import com.swasthai.app.ai.engine.ImageClassifier
import com.swasthai.app.ai.engine.LocalRagRetriever
import com.swasthai.app.ai.engine.ModelLoader
import com.swasthai.app.ai.engine.RagKnowledgeRepository
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
    fun provideRagKnowledgeRepository(
        @ApplicationContext context: Context
    ): RagKnowledgeRepository = RagKnowledgeRepository(context)

    @Provides
    @Singleton
    fun provideLocalRagRetriever(
        repository: RagKnowledgeRepository
    ): LocalRagRetriever = LocalRagRetriever(repository.buildDocuments())

    @Provides
    @Singleton
    fun provideClinicalReasoningEngine(
        ragRetriever: LocalRagRetriever
    ): ClinicalReasoningEngine = ClinicalReasoningEngine(ragRetriever)

    @Provides
    @Singleton
    fun provideAIEngineManager(
        modelLoader: ModelLoader,
        imageClassifier: ImageClassifier,
        reasoningEngine: ClinicalReasoningEngine,
        gemmaFallback: GemmaFallbackClient,
        capabilityGate: DeviceCapabilityGate
    ): AIEngineManager = AIEngineManager(
        modelLoader, imageClassifier, reasoningEngine, gemmaFallback, capabilityGate
    )
}
