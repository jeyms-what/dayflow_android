package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_schedule_proposals",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAtEpochMillis"])
    ]
)
data class AiScheduleProposalEntity(
    @PrimaryKey val id: String,
    // LOCAL_GEMMA, LOCAL_RULES, CLOUD_AI
    val engine: String,
    val modelId: String? = null,
    // DRAFT, APPLIED, REJECTED, EXPIRED
    val status: String = "DRAFT",
    val summary: String,
    val inputSnapshotJson: String,
    val baseDataFingerprint: String,
    val promptVersion: String? = null,
    val createdAtEpochMillis: Long,
    val appliedAtEpochMillis: Long? = null
)
