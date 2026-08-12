package com.swasthai.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.database.entity.DiagnosisResultEntity
import com.swasthai.app.data.local.database.entity.ImageRecordEntity
import com.swasthai.app.data.local.database.entity.RecommendationEntity
import com.swasthai.app.data.local.database.entity.ScreeningEntity
import com.swasthai.app.data.local.database.entity.SymptomRecordEntity
import com.swasthai.app.data.local.database.entity.VitalsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Screening and related operations.
 * Handles the complete screening workflow data.
 */
@Dao
interface ScreeningDao {

    // ── Screening ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreening(screening: ScreeningEntity)

    @Update
    suspend fun updateScreening(screening: ScreeningEntity)

    @Query("SELECT * FROM screenings WHERE id = :screeningId")
    suspend fun getScreeningById(screeningId: String): ScreeningEntity?

    @Query("SELECT * FROM screenings WHERE user_id = :userId ORDER BY created_at DESC")
    fun getScreeningsByUser(userId: String): Flow<List<ScreeningEntity>>

    @Query("SELECT * FROM screenings WHERE patient_id = :patientId ORDER BY created_at DESC")
    fun getScreeningsByPatient(patientId: String): Flow<List<ScreeningEntity>>

    @Query("SELECT * FROM screenings WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    fun getRecentScreenings(userId: String, limit: Int = 5): Flow<List<ScreeningEntity>>

    @Query("SELECT COUNT(*) FROM screenings WHERE user_id = :userId AND created_at >= :startOfDay")
    suspend fun getTodayScreeningCount(userId: String, startOfDay: Long): Int

    @Query("SELECT * FROM screenings WHERE is_synced = 0")
    suspend fun getUnsyncedScreenings(): List<ScreeningEntity>

    @Query("UPDATE screenings SET is_synced = 1 WHERE id = :screeningId")
    suspend fun markAsSynced(screeningId: String)

    // ── Vitals ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitals(vitals: VitalsEntity)

    @Query("SELECT * FROM vitals WHERE screening_id = :screeningId")
    suspend fun getVitalsByScreening(screeningId: String): VitalsEntity?

    // ── Symptoms ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomRecordEntity>)

    @Query("SELECT * FROM symptom_records WHERE screening_id = :screeningId")
    suspend fun getSymptomsByScreening(screeningId: String): List<SymptomRecordEntity>

    // ── Images ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageRecord(image: ImageRecordEntity)

    @Query("SELECT * FROM image_records WHERE screening_id = :screeningId")
    suspend fun getImagesByScreening(screeningId: String): List<ImageRecordEntity>

    // ── Diagnosis Results ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosisResult(result: DiagnosisResultEntity)

    @Query("SELECT * FROM diagnosis_results WHERE screening_id = :screeningId")
    suspend fun getDiagnosisByScreening(screeningId: String): DiagnosisResultEntity?

    // ── Recommendations ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Query("SELECT * FROM recommendations WHERE diagnosis_id = :diagnosisId ORDER BY priority ASC")
    suspend fun getRecommendationsByDiagnosis(diagnosisId: String): List<RecommendationEntity>

    // ── Analytics queries (Health Worker) ──

    @Query("""
        SELECT COUNT(*) FROM screenings 
        WHERE user_id = :userId 
        AND created_at BETWEEN :startDate AND :endDate
    """)
    suspend fun getScreeningCountInRange(userId: String, startDate: Long, endDate: Long): Int

    @Query("""
        SELECT COUNT(*) FROM diagnosis_results dr
        INNER JOIN screenings s ON dr.screening_id = s.id
        WHERE s.user_id = :userId 
        AND dr.risk_level = 'HIGH'
        AND s.created_at BETWEEN :startDate AND :endDate
    """)
    suspend fun getHighRiskCountInRange(userId: String, startDate: Long, endDate: Long): Int
}
