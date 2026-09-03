package com.swasthai.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swasthai.app.data.local.database.entity.ConsultationRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for online consultation requests (tracker).
 */
@Dao
interface ConsultationRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ConsultationRequestEntity)

    @Query("SELECT * FROM consultation_requests WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUser(userId: String): Flow<List<ConsultationRequestEntity>>

    @Query("SELECT * FROM consultation_requests WHERE id = :id")
    suspend fun getById(id: String): ConsultationRequestEntity?

    @Query("UPDATE consultation_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}