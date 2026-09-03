package com.swasthai.app.core.ai

import com.swasthai.app.ai.engine.LocalRagRetriever
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time, off-main-thread warmup of the on-device AI primitives at app start.
 *
 * Constructing [LocalRagRetriever] builds the small RAG index (token maps over
 * the bundled clinical + general health dataset); touching it here means the
 * first chat message or screening never pays the index-build latency on the
 * UI thread. Runs on a daemon thread so it never delays first frame.
 */
@Singleton
class StartupWarmup @Inject constructor(
    private val ragRetriever: LocalRagRetriever
) {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "swasthai-warmup").apply { isDaemon = true }
    }

    fun warm() {
        executor.execute {
            runCatching {
                // Force index construction + a real retrieval pass.
                ragRetriever.retrieve("health tips immunity fever cold", limit = 3)
            }
        }
    }
}