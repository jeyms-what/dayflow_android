package com.dayflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_proposal_blocks",
    foreignKeys = [
        ForeignKey(
            entity = AiScheduleProposalEntity::class,
            parentColumns = ["id"],
            childColumns = ["proposalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["proposalId"]),
        Index(value = ["startAtEpochMillis"])
    ]
)
data class AiProposalBlockEntity(
    @PrimaryKey val id: String,
    val proposalId: String,
    // TASK, HABIT, BREAK
    val blockType: String,
    // Snapshot references; validate live records before applying.
    val taskId: String? = null,
    val habitId: String? = null,
    val titleSnapshot: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val timeZoneId: String,
    val rationale: String,
    val confidence: Float? = null,
    val position: Int
)
