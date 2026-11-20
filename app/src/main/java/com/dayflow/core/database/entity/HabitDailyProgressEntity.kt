package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_daily_progress",
    primaryKeys = ["habitId", "localDateIso"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["localDateIso"])]
)
data class HabitDailyProgressEntity(
    val habitId: String,
    // Local calendar date in YYYY-MM-DD format.
    val localDateIso: String,
    val timeZoneId: String,
    val completionCount: Int = 0,
    // Configuration snapshot at the time of progress.
    val targetCountAtTime: Int,
    val frequencyPeriodAtTime: String,
    val updatedAtEpochMillis: Long
)
