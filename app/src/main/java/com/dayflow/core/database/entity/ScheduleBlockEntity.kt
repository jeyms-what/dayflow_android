package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_blocks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["habitId"]),
        Index(value = ["startAtEpochMillis"]),
        Index(value = ["endAtEpochMillis"])
    ]
)
data class ScheduleBlockEntity(
    @PrimaryKey val id: String,
    // TASK, HABIT, BREAK, EXTERNAL
    val blockType: String,
    val taskId: String? = null,
    val habitId: String? = null,
    val titleSnapshot: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val timeZoneId: String,
    // PLANNED, IN_PROGRESS, COMPLETED, SKIPPED, CANCELLED
    val status: String = "PLANNED",
    // MANUAL, AI, IMPORTED
    val source: String = "MANUAL",
    val isMovable: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
