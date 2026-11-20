package com.dayflow.data.repository

import javax.inject.Inject
import com.dayflow.core.database.dao.AiProposalDao
import com.dayflow.data.mapper.toDomain
import com.dayflow.data.mapper.toEntity
import com.dayflow.domain.model.ProposalStatus
import com.dayflow.domain.model.ScheduleProposal
import com.dayflow.domain.repository.AiProposalRepository

class RoomAiProposalRepository @Inject constructor(
    private val proposalDao: AiProposalDao
) : AiProposalRepository {

    override suspend fun saveProposal(proposal: ScheduleProposal) {
        require(proposal.id.isNotBlank())
        require(proposal.summary.isNotBlank())
        require(proposal.blocks.isNotEmpty()) { "Proposal must contain at least one block." }
        require(proposal.blocks.map { it.id }.distinct().size == proposal.blocks.size) {
            "Proposal block IDs must be unique."
        }
        require(proposal.status == ProposalStatus.DRAFT) {
            "Only draft proposals can be saved as new proposals."
        }

        proposal.blocks.forEach { block ->
            require(block.endAtEpochMillis > block.startAtEpochMillis) {
                "Every proposed block must have a positive duration."
            }
        }

        proposalDao.saveProposal(
            proposal = proposal.toEntity(),
            blocks = proposal.blocks.map { it.toEntity(proposal.id) }
        )
    }

    override suspend fun getProposal(proposalId: String): ScheduleProposal? {
        val entity = proposalDao.getProposal(proposalId) ?: return null
        val blocks = proposalDao.getProposalBlocks(proposalId)
        return entity.toDomain(blocks)
    }

    override suspend fun rejectProposal(proposalId: String) {
        val entity = proposalDao.getProposal(proposalId)
            ?: throw IllegalArgumentException("Schedule proposal was not found.")
        check(entity.status == ProposalStatus.DRAFT.name) {
            "Only draft proposals can be rejected."
        }
        val updated = proposalDao.updateProposalStatus(
            proposalId = proposalId,
            status = ProposalStatus.REJECTED.name,
            appliedAt = null
        )
        check(updated == 1) { "Proposal could not be rejected." }
    }
}
