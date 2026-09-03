package com.swasthai.app.ai.engine

/**
 * Thin JNI bridge to the native C++ AI kernels (libswasthai_ai).
 *
 * The hybrid strategy: Kotlin orchestrates and normalizes, C++ runs the hot
 * scoring loop, and if the native library cannot load (32-bit weirdness,
 * missing lib) every call degrades gracefully to null — callers fall back to
 * the pure-Kotlin implementation. The app never depends on native success.
 */
object AiNative {

    private external fun nativeDocScore(
        queryTerms: Array<String>,
        docTokens: Array<String>
    ): Float

    /** True once the native library is loaded and usable. */
    val available: Boolean by lazy {
        try {
            System.loadLibrary("swasthai_ai")
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Native frequency-weighted overlap score, or null when unavailable or
     * on any failure. A negative native score also yields null so the caller
     * can transparently fall back to the Kotlin implementation.
     */
    fun docScoreOrNull(
        queryTerms: Array<String>,
        docTokens: Array<String>
    ): Float? {
        if (!available) return null
        return try {
            val score = nativeDocScore(queryTerms, docTokens)
            if (score < 0f) null else score
        } catch (_: Throwable) {
            null
        }
    }
}