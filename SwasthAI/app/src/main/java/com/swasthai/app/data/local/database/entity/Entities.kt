package com.swasthai.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for User table.
 * Stores both Citizen and Health Worker profiles.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val language: String = "en",
    @ColumnInfo(name = "password_hash")
    val passwordHash: String? = null,
    @ColumnInfo(name = "firebase_uid")
    val firebaseUid: String? = null,
    @ColumnInfo(name = "assigned_area")
    val assignedArea: String? = null,
    @ColumnInfo(name = "phc_chc")
    val phcChc: String? = null,
    @ColumnInfo(name = "profile_image_path")
    val profileImagePath: String? = null,
    @ColumnInfo(name = "aadhaar_masked")
    val aadhaarMasked: String? = null,
    val experience: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)

/**
 * Room entity for Patient table.
 * Patients are registered by Health Workers.
 */
@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "reference_id")
    val referenceId: String = "",
    @ColumnInfo(name = "registered_by")
    val registeredBy: String = "",
    val name: String,
    val age: Int? = null,
    val gender: String? = null,
    val phone: String? = null,
    val village: String? = null,
    val address: String? = null,
    @ColumnInfo(name = "aadhar_number")
    val aadharNumber: String? = null,
    @ColumnInfo(name = "medical_history")
    val medicalHistory: String? = null,
    @ColumnInfo(name = "existing_diseases")
    val existingDiseases: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)


/**
 * Room entity for Screening table.
 * Represents a complete screening session.
 */
@Entity(tableName = "screenings")
data class ScreeningEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "patient_id")
    val patientId: String? = null,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "screening_type")
    val screeningType: String,
    val status: String = "IN_PROGRESS",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)

/**
 * Room entity for Vitals table.
 */
@Entity(tableName = "vitals")
data class VitalsEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "screening_id")
    val screeningId: String,
    val temperature: Float? = null,
    val pulse: Int? = null,
    val spo2: Float? = null,
    @ColumnInfo(name = "blood_pressure")
    val bloodPressure: String? = null,
    val weight: Float? = null,
    val height: Float? = null,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for Symptom records.
 */
@Entity(tableName = "symptom_records")
data class SymptomRecordEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "screening_id")
    val screeningId: String,
    @ColumnInfo(name = "symptom_name")
    val symptomName: String,
    val duration: String? = null,
    val severity: String? = null,
    val source: String = "MANUAL"
)

/**
 * Room entity for Image records.
 */
@Entity(tableName = "image_records")
data class ImageRecordEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "screening_id")
    val screeningId: String,
    @ColumnInfo(name = "image_path")
    val imagePath: String,
    @ColumnInfo(name = "image_type")
    val imageType: String? = null,
    @ColumnInfo(name = "analysis_result")
    val analysisResult: String? = null,
    val confidence: Float? = null
)

/**
 * Room entity for Diagnosis Results.
 */
@Entity(tableName = "diagnosis_results")
data class DiagnosisResultEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "screening_id")
    val screeningId: String,
    @ColumnInfo(name = "predicted_disease")
    val predictedDisease: String,
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,
    @ColumnInfo(name = "risk_level")
    val riskLevel: String,
    @ColumnInfo(name = "differential_diagnosis")
    val differentialDiagnosis: String? = null, // JSON serialized list
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for Recommendations.
 */
@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "diagnosis_id")
    val diagnosisId: String,
    val text: String,
    val category: String = "general",
    val priority: Int = 0
)

/**
 * Room entity for Reports.
 */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "screening_id")
    val screeningId: String,
    @ColumnInfo(name = "patient_name")
    val patientName: String,
    @ColumnInfo(name = "report_content")
    val reportContent: String,
    @ColumnInfo(name = "pdf_path")
    val pdfPath: String? = null,
    @ColumnInfo(name = "generated_at")
    val generatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_shared")
    val isShared: Boolean = false
)

/**
 * Room entity for Referrals.
 */
@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "diagnosis_id")
    val diagnosisId: String,
    @ColumnInfo(name = "health_center")
    val healthCenter: String,
    @ColumnInfo(name = "referral_type")
    val referralType: String,
    val reason: String,
    val priority: String,
    val notes: String? = null,
    val status: String = "PENDING",
    @ColumnInfo(name = "scheduled_date")
    val scheduledDate: Long? = null
)

/**
 * Room entity for the Sync Queue.
 * Tracks data pending upload to the backend.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE
    val payload: String, // JSON serialized entity data
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    val status: String = "PENDING",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_attempted")
    val lastAttempted: Long? = null
)
