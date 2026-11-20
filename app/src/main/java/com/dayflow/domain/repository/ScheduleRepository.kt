package com.dayflow.domain.repository

import com.dayflow.domain.model.ScheduleBlock
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeTimeline(startAt: Long, endAt: Long): Flow<List<ScheduleBlock>>
    suspend fun getBlock(id: String): ScheduleBlock?
    suspend fun saveBlock(block: ScheduleBlock)
    suspend fun hasConflict(
        startAt: Long,
        endAt: Long,
        excludeBlockId: String? = null
    ): Boolean
    suspend fun applyProposal(proposalId: String)
}
