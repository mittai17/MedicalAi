package com.swasthai.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swasthai.app.data.local.database.entity.ExerciseSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExerciseSessionEntity)

    @Query("SELECT * FROM exercise_sessions WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY startTime ASC")
    fun getSessionsBetweenDates(userId: String, startDate: Long, endDate: Long): Flow<List<ExerciseSessionEntity>>
}
