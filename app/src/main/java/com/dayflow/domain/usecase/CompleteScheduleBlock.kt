package com.dayflow.domain.usecase

import com.dayflow.domain.model.ScheduleBlock
import com.dayflow.domain.repository.ScheduleRepository

class CompleteScheduleBlock(
    private val repository: ScheduleRepository,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(blockId: String): ScheduleBlock {
        require(blockId.isNotBlank()) { "Schedule block ID cannot be blank." }
        return repository.completeBlock(
            blockId = blockId,
            completedAtEpochMillis = now()
        )
    }
}
