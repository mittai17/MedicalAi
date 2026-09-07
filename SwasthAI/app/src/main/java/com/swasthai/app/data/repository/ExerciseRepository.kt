package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.ExerciseDao
import com.swasthai.app.data.local.database.entity.ExerciseSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ExerciseRepository {
    suspend fun recordSession(session: ExerciseSessionEntity)
    fun getSessionsBetweenDates(userId: String, startDate: Long, endDate: Long): Flow<List<ExerciseSessionEntity>>
}

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {
    override suspend fun recordSession(session: ExerciseSessionEntity) {
        exerciseDao.insertSession(session)
    }

    override fun getSessionsBetweenDates(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<ExerciseSessionEntity>> {
        return exerciseDao.getSessionsBetweenDates(userId, startDate, endDate)
    }
}
