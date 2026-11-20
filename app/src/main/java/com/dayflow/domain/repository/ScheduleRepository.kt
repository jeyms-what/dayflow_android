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

    /**
     * Completes a scheduled block and its linked task/habit record atomically.
     * Returns the updated block, or throws if it is missing/already completed.
     */
    suspend fun completeBlock(
        blockId: String,
        completedAtEpochMillis: Long
    ): ScheduleBlock

    suspend fun applyProposal(proposalId: String)
}
