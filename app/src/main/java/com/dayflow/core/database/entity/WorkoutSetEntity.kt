package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    indices = [Index(value = ["sessionId"]), Index(value = ["exerciseName"])]
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAtEpochMillis: Long
)
