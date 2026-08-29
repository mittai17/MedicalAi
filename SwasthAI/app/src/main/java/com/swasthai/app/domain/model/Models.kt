package com.swasthai.app.domain.model

/**
 * Domain model representing a user (Citizen or Health Worker).
 */
data class User(
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole,
    val language: String = "en",
    val firebaseUid: String? = null,
    val assignedArea: String? = null,
    val phcChc: String? = null,
    val profileImagePath: String? = null,
    val aadhaarMasked: String? = null,
    val experience: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Domain model representing a patient registered by a Health Worker.
 */
data class Patient(
    val id: String,
    val referenceId: String = "",
    val registeredBy: String = "",
    val name: String,
    val age: Int? = null,
    val gender: String? = null,
    val phone: String? = null,
    val village: String? = null,
    val address: String? = null,
    val aadharNumber: String? = null,
    val medicalHistory: String? = null,
    val existingDiseases: List<String> = emptyList(),
    val lastScreeningDate: Long? = null,
    val screeningCount: Int = 0,
    val visitCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Domain model representing a screening session.
 */
data class Screening(
    val id: String,
    val patientId: String? = null,
    val userId: String,
    val screeningType: ScreeningType,
    val status: ScreeningStatus = ScreeningStatus.IN_PROGRESS,
    val symptoms: List<Symptom> = emptyList(),
    val vitals: Vitals? = null,
    val diagnosisResult: DiagnosisResult? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Domain model for recorded vital signs.
 */
data class Vitals(
    val id: String,
    val screeningId: String,
    val temperature: Float? = null,
    val pulse: Int? = null,
    val spo2: Float? = null,
    val bloodPressure: String? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

/**
 * Domain model for a recorded symptom.
 */
data class Symptom(
    val id: String,
    val screeningId: String,
    val name: String,
    val duration: String? = null,
    val severity: String? = null,
    val source: SymptomSource = SymptomSource.MANUAL
)

/**
 * Domain model for AI diagnosis result.
 */
data class DiagnosisResult(
    val id: String,
    val screeningId: String,
    val predictedDisease: String,
    val confidenceScore: Float,
    val riskLevel: RiskLevel,
    val differentialDiagnosis: List<String> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val medicalAdvice: MedicalAdvice? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model for the AI doctor's explanation of a condition.
 *
 * Provides the "why" (cause), the "what to do" (remedy) and the
 * "who to see" (doctor to consult) for a predicted condition.
 */
data class MedicalAdvice(
    val condition: String,
    val cause: String,
    val remedy: String,
    val doctorToConsult: String,
    val urgencyHint: String = ""
)

/**
 * Domain model for a medical recommendation.
 */
data class Recommendation(
    val id: String,
    val diagnosisId: String,
    val text: String,
    val category: String = "general",
    val priority: Int = 0
)

/**
 * Domain model for a medical report generated from screening.
 */
data class Report(
    val id: String,
    val screeningId: String,
    val patientName: String,
    val reportContent: String,
    val pdfPath: String? = null,
    val generatedAt: Long = System.currentTimeMillis(),
    val isShared: Boolean = false
)

/**
 * Domain model for a patient referral to a PHC/Hospital.
 */
data class Referral(
    val id: String,
    val diagnosisId: String,
    val patientName: String = "",
    val healthCenter: String,
    val facilityName: String = healthCenter,
    val referralType: String,
    val reason: String,
    val priority: String = "NORMAL",
    val notes: String? = null,
    val status: ReferralStatus = ReferralStatus.PENDING,
    val scheduledDate: Long? = null
)
