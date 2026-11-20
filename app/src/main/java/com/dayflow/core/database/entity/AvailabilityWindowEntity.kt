package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "availability_windows",
    indices = [Index(value = ["daysOfWeekMask"])]
)
data class AvailabilityWindowEntity(
    @PrimaryKey val id: String,
    val title: String,
    // AVAILABLE or UNAVAILABLE
    val windowType: String,
    // Local minutes after midnight: 0..1439.
    val startMinute: Int,
    val endMinute: Int,
    // Monday = bit 0 through Sunday = bit 6.
    val daysOfWeekMask: Int,
    val timeZoneId: String,
    val validFromDateIso: String? = null,
    val validUntilDateIso: String? = null,
    val isHardConstraint: Boolean = true,
    val isEnabled: Boolean = true
)
