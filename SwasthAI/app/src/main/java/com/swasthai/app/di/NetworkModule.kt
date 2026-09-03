package com.swasthai.app.di

import com.swasthai.app.BuildConfig
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.data.remote.SessionTokenProvider
import com.swasthai.app.data.remote.api.SwasthAIApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies for Ktor.
 *
 * Configures a Ktor [HttpClient] (Android engine) with timeouts, common
 * JSON headers and the anonymous device id used for sync. SwasthAI has no
 * auth token — identity is the stable per-device id in `X-Device-Id`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSessionTokenProvider(
        userPreferences: UserPreferences
    ): SessionTokenProvider = SessionTokenProvider(userPreferences)

    @Provides
    @Singleton
    fun provideHttpClient(
        sessionTokenProvider: SessionTokenProvider
    ): HttpClient = HttpClient(Android) {
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }

        if (BuildConfig.DEBUG) {
            install(Logging) {
                level = LogLevel.BODY
            }
        }

        defaultRequest {
            val deviceId = sessionTokenProvider.cachedDeviceId
            if (deviceId.isNotBlank()) {
                headers["X-Device-Id"] = deviceId
            }
            contentType(ContentType.Application.Json)
        }
    }

    @Provides
    @Singleton
    fun provideApiService(
        httpClient: HttpClient,
        sessionTokenProvider: SessionTokenProvider
    ): SwasthAIApiService = SwasthAIApiService(httpClient, sessionTokenProvider)
}