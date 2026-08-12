package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.PatientDao
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.local.database.entity.SyncQueueEntity
import com.swasthai.app.data.mapper.toDomain
import com.swasthai.app.data.mapper.toEntity
import com.swasthai.app.domain.model.Patient
import com.swasthai.app.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of PatientRepository.
 *
 * All write operations automatically queue to the sync queue
 * for background synchronization when internet becomes available.
 */
class PatientRepositoryImpl @Inject constructor(
    private val patientDao: PatientDao,
    private val syncQueueDao: SyncQueueDao
) : PatientRepository {

    override suspend fun registerPatient(patient: Patient): Result<Unit> {
        return try {
            patientDao.insertPatient(patient.toEntity())
            queueSync("patients", patient.id, "CREATE")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePatient(patient: Patient): Result<Unit> = registerPatient(patient)

    override suspend fun updatePatient(patient: Patient): Result<Unit> {
        return try {
            patientDao.updatePatient(patient.toEntity())
            queueSync("patients", patient.id, "UPDATE")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPatientById(patientId: String): Patient? {
        return patientDao.getPatientById(patientId)?.toDomain()
    }

    override fun getPatientsByHealthWorker(healthWorkerId: String): Flow<List<Patient>> {
        return patientDao.getPatientsByHealthWorker(healthWorkerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllPatients(): Flow<List<Patient>> {
        return patientDao.getAllPatients().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchPatients(query: String): Flow<List<Patient>> {
        return patientDao.searchPatients(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPatientCount(healthWorkerId: String): Int {
        return patientDao.getPatientCount(healthWorkerId)
    }

    private suspend fun queueSync(entityType: String, entityId: String, action: String) {
        syncQueueDao.insertSyncItem(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = "" // Will be populated by SyncWorker
            )
        )
    }
}
