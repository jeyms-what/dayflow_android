package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["frequencyPeriod"])
    ]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    // DAY or WEEK
    val frequencyPeriod: String,
    val targetCount: Int,
    // Monday = bit 0 through Sunday = bit 6; zero means any day.
    val selectedDaysMask: Int = 0,
    val startDateIso: String,
    val endDateIso: String? = null,
    // Local minute-of-day: 0..1439.
    val preferredStartMinuteOfDay: Int? = null,
    val durationMinutes: Int = 15,
    val energyLevel: String = "MEDIUM",
    val reminderEnabled: Boolean = false,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
