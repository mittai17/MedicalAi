package com.swasthai.app.data.repository

import com.swasthai.app.core.reminders.FollowUpScheduler
import com.swasthai.app.data.local.database.dao.ScreeningDao
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.local.database.entity.SyncQueueEntity
import com.swasthai.app.data.mapper.*
import com.swasthai.app.domain.model.*
import com.swasthai.app.domain.repository.ScreeningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of ScreeningRepository.
 *
 * Manages the complete screening workflow including symptoms,
 * vitals, diagnosis results, and recommendations.
 */
class ScreeningRepositoryImpl @Inject constructor(
    private val screeningDao: ScreeningDao,
    private val syncQueueDao: SyncQueueDao,
    private val followUpScheduler: FollowUpScheduler
) : ScreeningRepository {

    override suspend fun createScreening(screening: Screening): Result<String> {
        return try {
            screeningDao.insertScreening(screening.toEntity())
            queueSync("screenings", screening.id, "CREATE")
            Result.success(screening.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateScreening(screening: Screening): Result<Unit> {
        return try {
            screeningDao.updateScreening(screening.toEntity())
            queueSync("screenings", screening.id, "UPDATE")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getScreeningById(screeningId: String): Screening? {
        return screeningDao.getScreeningById(screeningId)?.toDomain()
    }

    override suspend fun getScreeningDetail(screeningId: String): ScreeningDetail? {
        val entity = screeningDao.getScreeningById(screeningId) ?: return null
        val screening = entity.toDomain()
        val symptoms = screeningDao.getSymptomsByScreening(screeningId).map { it.toDomain() }
        val vitals = screeningDao.getVitalsByScreening(screeningId)?.toDomain()
        val diagnosis = screeningDao.getDiagnosisByScreening(screeningId)?.let { result ->
            val recommendations = screeningDao
                .getRecommendationsByDiagnosis(result.id)
                .map { it.toDomain() }
            result.toDomain(recommendations)
        }
        val images = screeningDao.getImagesByScreening(screeningId).map { it.toDomain() }
        return ScreeningDetail(
            screening = screening,
            symptoms = symptoms,
            vitals = vitals,
            diagnosis = diagnosis,
            images = images
        )
    }

    override fun getScreeningsByUser(userId: String): Flow<List<Screening>> {
        return screeningDao.getScreeningsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentScreenings(userId: String, limit: Int): Flow<List<Screening>> {
        return screeningDao.getRecentScreenings(userId, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestVitals(userId: String): Flow<Vitals?> {
        return screeningDao.getLatestVitalsByUser(userId).map { it?.toDomain() }
    }

    override suspend fun saveVitals(vitals: Vitals): Result<Unit> {
        return try {
            screeningDao.insertVitals(vitals.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSymptoms(symptoms: List<Symptom>): Result<Unit> {
        return try {
            screeningDao.insertSymptoms(symptoms.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDiagnosisResult(result: DiagnosisResult): Result<Unit> {
        return try {
            screeningDao.insertDiagnosisResult(result.toEntity())
            if (result.recommendations.isNotEmpty()) {
                screeningDao.insertRecommendations(result.recommendations.map { it.toEntity() })
            }
            // Schedule a one-shot follow-up for flagged risk (fires the next day at 10:00).
            followUpScheduler.scheduleFollowUp(
                screeningId = result.screeningId,
                riskLevel = result.riskLevel,
                disease = result.predictedDisease,
                hint = result.medicalAdvice?.doctorToConsult
                    ?.let { "Consult a $it for follow-up." }
                    ?: "Follow up with a health professional about ${result.predictedDisease}."
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTodayScreeningCount(userId: String): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return screeningDao.getTodayScreeningCount(userId, calendar.timeInMillis)
    }

    override suspend fun getScreeningCountInRange(userId: String, startDate: Long, endDate: Long): Int {
        return screeningDao.getScreeningCountInRange(userId, startDate, endDate)
    }

    override suspend fun getHighRiskCountInRange(userId: String, startDate: Long, endDate: Long): Int {
        return screeningDao.getHighRiskCountInRange(userId, startDate, endDate)
    }

    private suspend fun queueSync(entityType: String, entityId: String, action: String) {
        syncQueueDao.insertSyncItem(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = ""
            )
        )
    }
}
