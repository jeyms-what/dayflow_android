package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["startedAtEpochMillis"]), Index(value = ["status"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val durationSeconds: Long = 0,
    val notes: String = "",
    val status: String = "COMPLETED",
    val createdAtEpochMillis: Long
)
