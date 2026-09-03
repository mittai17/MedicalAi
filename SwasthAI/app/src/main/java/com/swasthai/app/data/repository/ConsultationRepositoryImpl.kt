package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.ConsultationRequestDao
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.local.database.entity.SyncQueueEntity
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.data.mapper.toDomain
import com.swasthai.app.data.mapper.toEntity
import com.swasthai.app.domain.model.ConsultationRequest
import com.swasthai.app.domain.model.ConsultationRequestStatus
import com.swasthai.app.domain.repository.ConsultationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ConsultationRepository].
 *
 * Consultation requests are offline-first: they are stored locally and queued
 * for backend sync. Status is treated as local until the backend confirms it.
 */
@Singleton
class ConsultationRepositoryImpl @Inject constructor(
    private val consultationRequestDao: ConsultationRequestDao,
    private val syncQueueDao: SyncQueueDao,
    private val userPreferences: UserPreferences
) : ConsultationRepository {

    override suspend fun submitRequest(request: ConsultationRequest): Result<Unit> {
        return try {
            val deviceId = userPreferences.stableUserId()
            val entity = request.copy(userId = deviceId).toEntity()
            consultationRequestDao.insert(entity)
            queueSync("consultations", entity.id, "CREATE")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRequestsByUser(userId: String): Flow<List<ConsultationRequest>> {
        return consultationRequestDao.getByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateStatus(id: String, status: ConsultationRequestStatus) {
        consultationRequestDao.updateStatus(id, status.name)
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