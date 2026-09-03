package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.ConsultationRequestDao
import com.swasthai.app.data.local.database.dao.PatientDao
import com.swasthai.app.data.local.database.dao.ReportDao
import com.swasthai.app.data.local.database.dao.ScreeningDao
import com.swasthai.app.data.local.database.entity.SyncQueueEntity
import com.swasthai.app.data.remote.api.SwasthAIApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns one [SyncQueueEntity] into a real backend upload.
 *
 * The sync queue stores only a lightweight {entityType, entityId} reference
 * (payload is empty), so every upload re-fetches the current record from Room
 * at send time and builds the JSON body. Returns failure so the caller can
 * mark the item failed + retry; unknown entity types and records already
 * deleted locally succeed so they don't retry forever.
 */
@Singleton
class SyncUploader @Inject constructor(
    private val screeningDao: ScreeningDao,
    private val reportDao: ReportDao,
    private val patientDao: PatientDao,
    private val consultationRequestDao: ConsultationRequestDao,
    private val apiService: SwasthAIApiService
) {

    suspend fun upload(item: SyncQueueEntity): Result<Unit> = try {
        when (item.entityType.uppercase()) {
            "SCREENING", "SCREENINGS" -> uploadScreening(item)
            "PATIENT", "PATIENTS" -> uploadPatient(item)
            "REPORT", "REPORTS" -> uploadReport(item)
            "CONSULTATION", "CONSULTATIONS" -> uploadConsultation(item)
            else -> Result.success(Unit) // Unknown type — nothing to sync.
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun uploadScreening(item: SyncQueueEntity): Result<Unit> {
        val screening = screeningDao.getScreeningById(item.entityId)
            ?: return Result.success(Unit) // Deleted locally — nothing to send.

        val body = mutableMapOf<String, Any?>(
            "id" to screening.id,
            "userId" to screening.userId,
            "patientId" to screening.patientId,
            "screeningType" to screening.screeningType,
            "status" to screening.status,
            "createdAt" to screening.createdAt
        )

        screeningDao.getVitalsByScreening(screening.id)?.let { vitals ->
            body["vitals"] = mapOf(
                "temperature" to vitals.temperature,
                "pulse" to vitals.pulse,
                "spo2" to vitals.spo2,
                "bloodPressure" to vitals.bloodPressure,
                "weight" to vitals.weight,
                "height" to vitals.height,
                "recordedAt" to vitals.recordedAt
            )
        }

        val symptoms = screeningDao.getSymptomsByScreening(screening.id).map {
            mapOf(
                "name" to it.symptomName,
                "duration" to it.duration,
                "severity" to it.severity,
                "source" to it.source
            )
        }
        if (symptoms.isNotEmpty()) body["symptoms"] = symptoms

        screeningDao.getDiagnosisByScreening(screening.id)?.let { diagnosis ->
            body["diagnosis"] = mapOf(
                "id" to diagnosis.id,
                "predictedDisease" to diagnosis.predictedDisease,
                "confidenceScore" to diagnosis.confidenceScore,
                "riskLevel" to diagnosis.riskLevel,
                "differentialDiagnosis" to diagnosis.differentialDiagnosis,
                "createdAt" to diagnosis.createdAt
            )
            val recommendations = screeningDao.getRecommendationsByDiagnosis(diagnosis.id).map {
                mapOf("text" to it.text, "category" to it.category, "priority" to it.priority)
            }
            if (recommendations.isNotEmpty()) body["recommendations"] = recommendations
        }

        val ok = if (item.action.equals("UPDATE", ignoreCase = true)) {
            apiService.updateScreening(screening.id, body)
        } else {
            apiService.uploadScreening(body)
        }
        return if (ok) Result.success(Unit) else Result.failure(IllegalStateException("Upload rejected"))
    }

    private suspend fun uploadPatient(item: SyncQueueEntity): Result<Unit> {
        val patient = patientDao.getPatientById(item.entityId)
            ?: return Result.success(Unit)

        val body = mapOf<String, Any?>(
            "id" to patient.id,
            "referenceId" to patient.referenceId,
            "registeredBy" to patient.registeredBy,
            "name" to patient.name,
            "age" to patient.age,
            "gender" to patient.gender,
            "phone" to patient.phone,
            "village" to patient.village,
            "address" to patient.address,
            "medicalHistory" to patient.medicalHistory,
            "existingDiseases" to patient.existingDiseases,
            "createdAt" to patient.createdAt
        )

        val ok = if (item.action.equals("UPDATE", ignoreCase = true)) {
            apiService.updatePatient(patient.id, body)
        } else {
            apiService.uploadPatient(body)
        }
        return if (ok) Result.success(Unit) else Result.failure(IllegalStateException("Upload rejected"))
    }

    private suspend fun uploadReport(item: SyncQueueEntity): Result<Unit> {
        // Queue entries for reports reference the screening they belong to.
        val report = reportDao.getReportByScreening(item.entityId)
            ?: return Result.success(Unit)

        val body = mapOf<String, Any?>(
            "id" to report.id,
            "screeningId" to report.screeningId,
            "patientName" to report.patientName,
            "reportContent" to report.reportContent,
            "generatedAt" to report.generatedAt
        )

        return if (apiService.uploadReport(body)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Upload rejected"))
        }
    }

    private suspend fun uploadConsultation(item: SyncQueueEntity): Result<Unit> {
        val request = consultationRequestDao.getById(item.entityId)
            ?: return Result.success(Unit) // Deleted locally — nothing to send.

        val body = mapOf<String, Any?>(
            "id" to request.id,
            "userId" to request.userId,
            "screeningId" to request.screeningId,
            "reason" to request.reason,
            "urgency" to request.urgency,
            "patientName" to request.patientName,
            "patientAge" to request.patientAge,
            "patientSex" to request.patientSex,
            "patientConditions" to request.patientConditions,
            "status" to request.status,
            "createdAt" to request.createdAt
        )

        return if (apiService.uploadConsultation(body)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Upload rejected"))
        }
    }
}