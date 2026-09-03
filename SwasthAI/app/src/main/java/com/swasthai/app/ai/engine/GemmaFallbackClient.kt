package com.swasthai.app.ai.engine

import com.swasthai.app.data.remote.api.SwasthAIApiService
import com.swasthai.app.domain.model.Symptom
import com.swasthai.app.domain.model.Vitals
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional remote Gemma fallback for low-end devices.
 *
 * The SwasthAI app is offline-first and does all heavy inference on-device so it
 * runs on low-end phones without a multi-GB model download. As a best-effort
 * second opinion, [getFallback] asks the local backend's `/ai/fallback` endpoint,
 * which can call a hosted Gemma model when configured.
 *
 * This class never throws and never blocks the screening flow: any network,
 * parsing or backend error simply returns null and the app keeps the
 * on-device result.
 */
@Singleton
class GemmaFallbackClient @Inject constructor(
    private val apiService: SwasthAIApiService
) {

    /**
     * Result of a fallback lookup, or null if unavailable.
     */
    data class FallbackResult(
        val predictedDisease: String,
        val advice: String?,
        val provider: String,
        val confidence: Float?
    )

    /**
     * Attempt a remote fallback. Returns null on any failure (offline,
     * no backend, backend without Gemma configured, etc.).
     */
    suspend fun getFallback(
        symptoms: List<Symptom>,
        vitals: Vitals?,
        voiceTranscript: String?,
        scanType: ScanType
    ): FallbackResult? {
        return try {
        val symptomNames = symptoms.map { it.name }

        val body = apiService.getAiFallback(
            mapOf(
                "symptoms" to symptomNames,
                "vitals" to buildVitalsMap(vitals),
                "voiceTranscript" to (voiceTranscript ?: ""),
                "scanType" to scanType.name
            )
        ) ?: return null

        val predicted = body.optString("predictedDisease").ifBlank { return null }

        FallbackResult(
            predictedDisease = predicted,
            advice = body.optString("advice").ifBlank { null },
            provider = body.optString("provider", "unknown"),
            confidence = if (body.has("confidence")) body.optDouble("confidence").toFloat() else null
        )
        } catch (e: Exception) {
            null
        }
    }

    private fun buildVitalsMap(vitals: Vitals?): Map<String, Any?> = if (vitals == null) {
        emptyMap()
    } else {
        mapOf(
            "temperature" to vitals.temperature,
            "spo2" to vitals.spo2,
            "pulse" to vitals.pulse,
            "bloodPressure" to (vitals.bloodPressure ?: "")
        )
    }
}
