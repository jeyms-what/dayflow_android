package com.dayflow.domain.repository

import com.dayflow.domain.model.ScheduleProposal

interface AiProposalRepository {
    suspend fun saveProposal(proposal: ScheduleProposal)
    suspend fun getProposal(proposalId: String): ScheduleProposal?
    suspend fun rejectProposal(proposalId: String)
}
