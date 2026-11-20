package com.dayflow.domain.usecase

import com.dayflow.domain.model.*
import com.dayflow.domain.repository.ScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompleteScheduleBlockTest {

    private class FakeScheduleRepository : ScheduleRepository {
        var completedId: String? = null
        var completedAt: Long? = null
        private val block = ScheduleBlock(
            id = "block-1",
            blockType = ScheduleBlockType.TASK,
            taskId = "task-1",
            habitId = null,
            title = "Study Kotlin",
            startAtEpochMillis = 1_000L,
            endAtEpochMillis = 2_000L,
            timeZoneId = "Asia/Manila",
            status = ScheduleBlockStatus.COMPLETED,
            source = ScheduleSource.MANUAL,
            isMovable = true,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L
        )

        override fun observeTimeline(startAt: Long, endAt: Long): Flow<List<ScheduleBlock>> =
            emptyFlow()

        override suspend fun getBlock(id: String): ScheduleBlock? =
            block.takeIf { it.id == id }

        override suspend fun saveBlock(block: ScheduleBlock) = Unit

        override suspend fun hasConflict(
            startAt: Long,
            endAt: Long,
            excludeBlockId: String?
        ): Boolean = false

        override suspend fun completeBlock(
            blockId: String,
            completedAtEpochMillis: Long
        ): ScheduleBlock {
            completedId = blockId
            completedAt = completedAtEpochMillis
            return block
        }

        override suspend fun applyProposal(proposalId: String) = Unit
    }

    @Test
    fun completionDelegatesToRepositoryWithInjectedTimestamp() = runBlocking {
        val repository = FakeScheduleRepository()
        val useCase = CompleteScheduleBlock(
            repository = repository,
            now = { 987_654L }
        )

        val result = useCase("block-1")

        assertEquals("block-1", repository.completedId)
        assertEquals(987_654L, repository.completedAt)
        assertEquals(ScheduleBlockStatus.COMPLETED, result.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankBlockIdsAreRejected() = runBlocking {
        CompleteScheduleBlock(
            repository = FakeScheduleRepository(),
            now = { 1L }
        )(" ")
    }
}
