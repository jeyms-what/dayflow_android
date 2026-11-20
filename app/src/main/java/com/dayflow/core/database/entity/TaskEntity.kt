package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["deadlineAtEpochMillis"]),
        Index(value = ["updatedAtEpochMillis"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val estimatedDurationMinutes: Int,
    val priority: Int = 2,
    val energyLevel: String = "MEDIUM",
    val deadlineAtEpochMillis: Long? = null,
    val status: String = "OPEN",
    val completedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)
