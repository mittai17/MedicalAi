package com.swasthai.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val exerciseId: String,
    val exerciseName: String,
    val date: Long, // Start of day timestamp
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val completed: Boolean
)
