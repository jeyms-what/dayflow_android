package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dayflow.core.database.entity.AiProposalBlockEntity
import com.dayflow.core.database.entity.AiScheduleProposalEntity

@Dao
interface AiProposalDao {
    @Upsert
    suspend fun upsertProposal(proposal: AiScheduleProposalEntity)

    @Upsert
    suspend fun upsertBlocks(blocks: List<AiProposalBlockEntity>)

    @Transaction
    suspend fun saveProposal(
        proposal: AiScheduleProposalEntity,
        blocks: List<AiProposalBlockEntity>
    ) {
        upsertProposal(proposal)
        upsertBlocks(blocks)
    }

    @Query("""
        SELECT * FROM ai_schedule_proposals
        WHERE id = :proposalId
        LIMIT 1
    """)
    suspend fun getProposal(proposalId: String): AiScheduleProposalEntity?

    @Query("""
        SELECT * FROM ai_proposal_blocks
        WHERE proposalId = :proposalId
        ORDER BY position ASC
    """)
    suspend fun getProposalBlocks(
        proposalId: String
    ): List<AiProposalBlockEntity>

    @Query("""
        UPDATE ai_schedule_proposals
        SET status = :status, appliedAtEpochMillis = :appliedAt
        WHERE id = :proposalId
    """)
    suspend fun updateProposalStatus(
        proposalId: String,
        status: String,
        appliedAt: Long?
    ): Int
}
