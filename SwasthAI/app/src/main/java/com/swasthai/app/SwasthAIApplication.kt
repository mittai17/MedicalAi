package com.swasthai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * SwasthAI Application class.
 *
 * Entry point for the application. Annotated with @HiltAndroidApp to trigger
 * Hilt's code generation and provide the application-level dependency container.
 *
 * Implements WorkManager's Configuration.Provider to support Hilt-injected workers.
 */
@HiltAndroidApp
class SwasthAIApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG
                else android.util.Log.ERROR
            )
            .build()

    override fun onCreate() {
        super.onCreate()
        // Initialize SQLCipher native library
        System.loadLibrary("sqlcipher")
    }
}
