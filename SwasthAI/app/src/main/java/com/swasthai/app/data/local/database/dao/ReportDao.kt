package com.swasthai.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.database.entity.ReferralEntity
import com.swasthai.app.data.local.database.entity.ReportEntity
import com.swasthai.app.data.local.database.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Report operations.
 */
@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("SELECT * FROM reports WHERE screening_id = :screeningId")
    suspend fun getReportByScreening(screeningId: String): ReportEntity?

    @Query("SELECT * FROM reports ORDER BY generated_at DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE patient_name LIKE '%' || :query || '%' ORDER BY generated_at DESC")
    fun searchReports(query: String): Flow<List<ReportEntity>>
}

/**
 * Data Access Object for Referral operations.
 */
@Dao
interface ReferralDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: ReferralEntity)

    @Update
    suspend fun updateReferral(referral: ReferralEntity)

    @Query("SELECT * FROM referrals WHERE diagnosis_id = :diagnosisId")
    suspend fun getReferralByDiagnosis(diagnosisId: String): ReferralEntity?

    @Query("SELECT * FROM referrals WHERE status = 'PENDING' ORDER BY scheduled_date ASC")
    fun getPendingReferrals(): Flow<List<ReferralEntity>>

    @Query("SELECT COUNT(*) FROM referrals WHERE status = 'PENDING'")
    suspend fun getPendingReferralCount(): Int
}

/**
 * Data Access Object for Sync Queue operations.
 * Manages the offline-to-online synchronization queue.
 */
@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueEntity)

    @Update
    suspend fun updateSyncItem(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' AND retry_count < 5 ORDER BY created_at ASC")
    suspend fun getRetryableItems(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    fun getPendingCount(): Flow<Int>

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompletedItems()

    @Query("DELETE FROM sync_queue WHERE id = :itemId")
    suspend fun deleteSyncItem(itemId: String)

    @Query("UPDATE sync_queue SET status = 'COMPLETED' WHERE id = :itemId")
    suspend fun markAsCompleted(itemId: String)

    @Query("""
        UPDATE sync_queue 
        SET status = 'FAILED', retry_count = retry_count + 1, last_attempted = :timestamp 
        WHERE id = :itemId
    """)
    suspend fun markAsFailed(itemId: String, timestamp: Long = System.currentTimeMillis())
}
