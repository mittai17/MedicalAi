package com.swasthai.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.database.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Patient operations.
 * Used primarily by Health Workers to manage patient records.
 */
@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientById(patientId: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE registered_by = :healthWorkerId ORDER BY created_at DESC")
    fun getPatientsByHealthWorker(healthWorkerId: String): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients ORDER BY created_at DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patients WHERE registered_by = :healthWorkerId")
    suspend fun getPatientCount(healthWorkerId: String): Int

    @Query("SELECT * FROM patients WHERE is_synced = 0")
    suspend fun getUnsyncedPatients(): List<PatientEntity>

    @Query("UPDATE patients SET is_synced = 1 WHERE id = :patientId")
    suspend fun markAsSynced(patientId: String)

    @Query("DELETE FROM patients WHERE id = :patientId")
    suspend fun deletePatient(patientId: String)
}
