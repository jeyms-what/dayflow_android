package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleBlockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scheduleBlockId"], unique = true),
        Index(value = ["triggerAtEpochMillis"]),
        Index(value = ["status"])
    ]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val scheduleBlockId: String,
    val triggerAtEpochMillis: Long,
    // SCHEDULED, FIRING, SNOOZED, DISMISSED, COMPLETED, CANCELLED
    val status: String = "SCHEDULED",
    val snoozeUntilEpochMillis: Long? = null,
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val maxRingDurationSeconds: Int = 60,
    // Stable identifier for the PendingIntent request.
    val requestCode: Int,
    val lastTriggeredAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
