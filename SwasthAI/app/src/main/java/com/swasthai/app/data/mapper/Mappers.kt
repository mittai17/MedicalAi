package com.swasthai.app.data.mapper

import com.swasthai.app.data.local.database.entity.*
import com.swasthai.app.domain.model.*

/**
 * Mappers between Room entities and domain models.
 *
 * Keeps the data layer isolated from the domain layer,
 * ensuring entities can change without affecting business logic.
 */

// ═══════════════════════════════════════
// USER MAPPERS
// ═══════════════════════════════════════

fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    phone = phone,
    role = try { UserRole.valueOf(role) } catch (_: Exception) { UserRole.CITIZEN },
    language = language,
    firebaseUid = firebaseUid,
    assignedArea = assignedArea,
    phcChc = phcChc,
    profileImagePath = profileImagePath,
    aadhaarMasked = aadhaarMasked,
    experience = experience,
    createdAt = createdAt,
    isSynced = isSynced
)

fun User.toEntity(passwordHash: String? = null) = UserEntity(
    id = id,
    name = name,
    phone = phone,
    role = role.name,
    language = language,
    passwordHash = passwordHash,
    firebaseUid = firebaseUid,
    assignedArea = assignedArea,
    phcChc = phcChc,
    profileImagePath = profileImagePath,
    aadhaarMasked = aadhaarMasked,
    experience = experience,
    createdAt = createdAt,
    isSynced = isSynced
)

// ═══════════════════════════════════════
// PATIENT MAPPERS
// ═══════════════════════════════════════

fun PatientEntity.toDomain() = Patient(
    id = id,
    referenceId = referenceId.ifBlank { "REF-${Math.abs(id.hashCode() % 900000 + 100000)}" },
    registeredBy = registeredBy,
    name = name,
    age = age,
    gender = gender,
    phone = phone,
    village = village,
    address = address,
    aadharNumber = aadharNumber,
    medicalHistory = medicalHistory,
    existingDiseases = existingDiseases?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    createdAt = createdAt,
    isSynced = isSynced
)

fun Patient.toEntity() = PatientEntity(
    id = id,
    referenceId = referenceId.ifBlank { "REF-${Math.abs(id.hashCode() % 900000 + 100000)}" },
    registeredBy = registeredBy,
    name = name,
    age = age,
    gender = gender,
    phone = phone,
    village = village,
    address = address,
    aadharNumber = aadharNumber,
    medicalHistory = medicalHistory,
    existingDiseases = existingDiseases.joinToString(","),
    createdAt = createdAt,
    isSynced = isSynced
)


// ═══════════════════════════════════════
// SCREENING MAPPERS
// ═══════════════════════════════════════

fun ScreeningEntity.toDomain() = Screening(
    id = id,
    patientId = patientId,
    userId = userId,
    screeningType = try { ScreeningType.valueOf(screeningType) } catch (_: Exception) { ScreeningType.SYMPTOM_CHECK },
    status = try { ScreeningStatus.valueOf(status) } catch (_: Exception) { ScreeningStatus.IN_PROGRESS },
    createdAt = createdAt,
    isSynced = isSynced
)

fun Screening.toEntity() = ScreeningEntity(
    id = id,
    patientId = patientId,
    userId = userId,
    screeningType = screeningType.name,
    status = status.name,
    createdAt = createdAt,
    isSynced = isSynced
)

// ═══════════════════════════════════════
// VITALS MAPPERS
// ═══════════════════════════════════════

fun VitalsEntity.toDomain() = Vitals(
    id = id,
    screeningId = screeningId,
    temperature = temperature,
    pulse = pulse,
    spo2 = spo2,
    bloodPressure = bloodPressure,
    weight = weight,
    height = height,
    recordedAt = recordedAt
)

fun Vitals.toEntity() = VitalsEntity(
    id = id,
    screeningId = screeningId,
    temperature = temperature,
    pulse = pulse,
    spo2 = spo2,
    bloodPressure = bloodPressure,
    weight = weight,
    height = height,
    recordedAt = recordedAt
)

// ═══════════════════════════════════════
// SYMPTOM MAPPERS
// ═══════════════════════════════════════

fun SymptomRecordEntity.toDomain() = Symptom(
    id = id,
    screeningId = screeningId,
    name = symptomName,
    duration = duration,
    severity = severity,
    source = try { SymptomSource.valueOf(source) } catch (_: Exception) { SymptomSource.MANUAL }
)

fun Symptom.toEntity() = SymptomRecordEntity(
    id = id,
    screeningId = screeningId,
    symptomName = name,
    duration = duration,
    severity = severity,
    source = source.name
)

// ═══════════════════════════════════════
// DIAGNOSIS RESULT MAPPERS
// ═══════════════════════════════════════

fun DiagnosisResultEntity.toDomain() = DiagnosisResult(
    id = id,
    screeningId = screeningId,
    predictedDisease = predictedDisease,
    confidenceScore = confidenceScore,
    riskLevel = try { RiskLevel.valueOf(riskLevel) } catch (_: Exception) { RiskLevel.LOW },
    differentialDiagnosis = differentialDiagnosis?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
    createdAt = createdAt
)

fun DiagnosisResult.toEntity() = DiagnosisResultEntity(
    id = id,
    screeningId = screeningId,
    predictedDisease = predictedDisease,
    confidenceScore = confidenceScore,
    riskLevel = riskLevel.name,
    differentialDiagnosis = differentialDiagnosis.joinToString("|"),
    createdAt = createdAt
)

// ═══════════════════════════════════════
// RECOMMENDATION MAPPERS
// ═══════════════════════════════════════

fun RecommendationEntity.toDomain() = Recommendation(
    id = id,
    diagnosisId = diagnosisId,
    text = text,
    category = category,
    priority = priority
)

fun Recommendation.toEntity() = RecommendationEntity(
    id = id,
    diagnosisId = diagnosisId,
    text = text,
    category = category,
    priority = priority
)

// ═══════════════════════════════════════
// REPORT MAPPERS
// ═══════════════════════════════════════

fun ReportEntity.toDomain() = Report(
    id = id,
    screeningId = screeningId,
    patientName = patientName,
    reportContent = reportContent,
    pdfPath = pdfPath,
    generatedAt = generatedAt,
    isShared = isShared
)

fun Report.toEntity() = ReportEntity(
    id = id,
    screeningId = screeningId,
    patientName = patientName,
    reportContent = reportContent,
    pdfPath = pdfPath,
    generatedAt = generatedAt,
    isShared = isShared
)

// ═══════════════════════════════════════
// REFERRAL MAPPERS
// ═══════════════════════════════════════

fun ReferralEntity.toDomain() = Referral(
    id = id,
    diagnosisId = diagnosisId,
    healthCenter = healthCenter,
    referralType = referralType,
    reason = reason,
    priority = priority,
    notes = notes,
    status = try { ReferralStatus.valueOf(status) } catch (_: Exception) { ReferralStatus.PENDING },
    scheduledDate = scheduledDate
)

fun Referral.toEntity() = ReferralEntity(
    id = id,
    diagnosisId = diagnosisId,
    healthCenter = healthCenter,
    referralType = referralType,
    reason = reason,
    priority = priority,
    notes = notes,
    status = status.name,
    scheduledDate = scheduledDate
)
