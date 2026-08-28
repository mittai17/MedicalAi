package com.swasthai.app.ai.engine

import com.swasthai.app.domain.model.DiagnosisResult
import com.swasthai.app.domain.model.MedicalAdvice
import com.swasthai.app.domain.model.Recommendation
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Symptom
import com.swasthai.app.domain.model.Vitals
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates multi-modal Edge AI inference.
 *
 * Fuses inputs from symptoms, images and vitals to produce a unified
 * medical screening result with risk classification and recommendations.
 *
 * Image input is handled by the real on-device TFLite vision models.
 * Text (symptoms) and vitals are handled by [ClinicalReasoningEngine],
 * which computes evidence-based predictions and confidence — no mock logic.
 */
@Singleton
class AIEngineManager @Inject constructor(
    private val modelLoader: ModelLoader,
    private val imageClassifier: ImageClassifier,
    private val reasoningEngine: ClinicalReasoningEngine,
    private val gemmaFallback: GemmaFallbackClient,
    private val capabilityGate: DeviceCapabilityGate
) {

    /**
     * Current deployment mode for this device.
     *  - [DeviceCapabilityGate.DeviceMode.FULL] on capable (arm64) devices —
     *    Gemma 4 E2B-IT LLM is available as fallback.
     *  - [DeviceCapabilityGate.DeviceMode.UI_ONLY] on low-end / 32-bit devices —
     *    primary models + RAG only; UI/QA testing.
     */
    fun currentMode(): DeviceCapabilityGate.DeviceMode = capabilityGate.currentMode()

    /**
     * Run the full AI analysis pipeline.
     *
     * @param symptoms List of reported symptoms
     * @param vitals Patient vital signs
     * @param imagePath Optional path to medical image
     * @param screeningId The screening session ID
     * @param scanType Selected scan type
     * @return DiagnosisResult with predictions, risk level, and recommendations
     */
    suspend fun runDiagnosis(
        symptoms: List<Symptom>,
        vitals: Vitals?,
        imagePath: String? = null,
        voiceTranscript: String? = null,
        screeningId: String,
        scanType: ScanType = ScanType.PNEUMONIA
    ): DiagnosisResult {
        val diagnosisId = UUID.randomUUID().toString()

        // When an image is provided, the on-device vision model is the
        // primary signal; symptoms/vitals still contribute context.
        val classification = if (imagePath != null) {
            imageClassifier.classify(imagePath, scanType)
        } else null

        // Always run the clinical reasoning over symptoms + vitals.
        val reasoning = reasoningEngine.reason(symptoms, vitals, diagnosisId)

        val predictedDisease: String
        val riskLevel: RiskLevel
        val confidence: Float
        val differential: List<String>

        if (classification != null) {
            predictedDisease = classification.label
            confidence = (classification.confidence * 100).coerceIn(1f, 99f)
            // Risk is driven by the confidence gate: below the calibrated gate
            // the model is not confident enough to call a condition.
            riskLevel = when {
                classification.isConfident &&
                    classification.confidence >= 0.9f &&
                    classification.scanType.isBinary -> RiskLevel.HIGH
                classification.isConfident -> RiskLevel.MODERATE
                else -> RiskLevel.LOW
            }
            differential = getDifferentialForScan(scanType, classification)
        } else {
            // No usable image — the evidence-based reasoning engine decides.
            // Only on capable (arm64) devices, when the on-device primary is
            // weak, does the Gemma 4 E2B-IT fallback get consulted. On low-end
            // / 32-bit devices the app stays purely on-device (no LLM).
            val fallback = if (capabilityGate.canRunOnDeviceLlm() &&
                reasoning.confidence < 0.5f
            ) {
                gemmaFallback.getFallback(symptoms, vitals, voiceTranscript, scanType)
            } else null

            if (fallback != null && fallback.predictedDisease.isNotBlank()) {
                predictedDisease = fallback.predictedDisease
                confidence = (fallback.confidence ?: reasoning.confidence * 100f)
                    .coerceIn(0f, 99f)
                // Risk is bounded below by the on-device reasoning so a model
                // second opinion can never downgrade a conservative assessment.
                riskLevel = when (reasoning.riskLevel) {
                    RiskLevel.HIGH -> RiskLevel.HIGH
                    RiskLevel.MODERATE -> RiskLevel.MODERATE
                    else -> RiskLevel.LOW
                }
                differential = reasoning.differentialDiagnosis
            } else {
                predictedDisease = reasoning.predictedDisease
                confidence = (reasoning.confidence * 100).coerceIn(0f, 99f)
                riskLevel = reasoning.riskLevel
                differential = reasoning.differentialDiagnosis
            }
        }

        val recommendations = if (classification != null) {
            // Image path: combine image-driven risk actions with condition care.
            generateRecommendations(
                diagnosisId = diagnosisId,
                riskLevel = riskLevel,
                predictedDisease = predictedDisease,
                symptoms = symptoms
            )
        } else {
            reasoning.recommendations
        }

        return DiagnosisResult(
            id = diagnosisId,
            screeningId = screeningId,
            predictedDisease = predictedDisease,
            confidenceScore = confidence,
            riskLevel = riskLevel,
            differentialDiagnosis = differential,
            recommendations = recommendations,
            medicalAdvice = resolveAdvice(predictedDisease, scanType, reasoning.advice)
        )
    }

    /**
     * Differential diagnoses relevant to the selected scan type, excluding the
     * predicted label itself. For multilabel scans the detected findings are
     * the primary signal.
     */
    private fun getDifferentialForScan(scanType: ScanType, classification: ClassificationResult): List<String> {
        if (classification.findings.isNotEmpty()) {
            return classification.findings.filter { it != classification.label }
        }
        return scanType.labels.filter { it != classification.label }
    }

    /**
     * Resolve the AI doctor's explanation for the predicted condition.
     *
     * CT organ models identify an anatomical region, not a disease, so they
     * return non-diagnostic organ guidance. Text-path predictions use the
     * reasoning engine's evidence-based advice.
     */
    private fun resolveAdvice(
        predictedDisease: String,
        scanType: ScanType,
        reasoningAdvice: MedicalAdvice
    ): MedicalAdvice {
        val isOrganScan = scanType == ScanType.CT_ORGAN_AXIAL ||
            scanType == ScanType.CT_ORGAN_CORONAL ||
            scanType == ScanType.CT_ORGAN_SAGITTAL
        return when {
            isOrganScan -> DiseaseKnowledgeBase.organAdvice(predictedDisease)
            scanType == ScanType.PNEUMONIA ||
                scanType == ScanType.BREAST_SCAN ||
                scanType == ScanType.RETINA ||
                scanType == ScanType.SKIN_LESION ||
                scanType == ScanType.OCT_RETINA ||
                scanType == ScanType.BLOOD_CELL ||
                scanType == ScanType.COLON_PATH ||
                scanType == ScanType.CHEST_XRAY ||
                scanType == ScanType.KIDNEY_TISSUE -> DiseaseKnowledgeBase.adviceFor(predictedDisease)
            else -> reasoningAdvice
        }
    }

    /**
     * Image-path recommendations: risk-driven actions plus general care.
     * These are derived from the risk level, not fabricated per case.
     */
    private fun generateRecommendations(
        diagnosisId: String,
        riskLevel: RiskLevel,
        predictedDisease: String,
        symptoms: List<Symptom>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        var priority = 0

        // Risk-based recommendations
        when (riskLevel) {
            RiskLevel.HIGH -> {
                recommendations.add(
                    Recommendation(
                        id = UUID.randomUUID().toString(),
                        diagnosisId = diagnosisId,
                        text = "Seek immediate medical attention at the nearest health facility",
                        category = "urgent",
                        priority = priority++
                    )
                )
                recommendations.add(
                    Recommendation(
                        id = UUID.randomUUID().toString(),
                        diagnosisId = diagnosisId,
                        text = "If symptoms worsen, call emergency services (108)",
                        category = "emergency",
                        priority = priority++
                    )
                )
            }
            RiskLevel.MODERATE -> {
                recommendations.add(
                    Recommendation(
                        id = UUID.randomUUID().toString(),
                        diagnosisId = diagnosisId,
                        text = "Visit a health center within 24 hours for proper evaluation",
                        category = "action",
                        priority = priority++
                    )
                )
            }
            RiskLevel.LOW -> {
                recommendations.add(
                    Recommendation(
                        id = UUID.randomUUID().toString(),
                        diagnosisId = diagnosisId,
                        text = "Monitor symptoms and seek care if they worsen",
                        category = "monitoring",
                        priority = priority++
                    )
                )
            }
        }

        // General care recommendations
        recommendations.add(
            Recommendation(
                id = UUID.randomUUID().toString(),
                diagnosisId = diagnosisId,
                text = "Get enough rest and drink plenty of fluids",
                category = "care",
                priority = priority++
            )
        )

        if (riskLevel != RiskLevel.LOW) {
            recommendations.add(
                Recommendation(
                    id = UUID.randomUUID().toString(),
                    diagnosisId = diagnosisId,
                    text = "If symptoms worsen, visit a doctor",
                    category = "followup",
                    priority = priority++
                )
            )
        }

        return recommendations
    }
}
