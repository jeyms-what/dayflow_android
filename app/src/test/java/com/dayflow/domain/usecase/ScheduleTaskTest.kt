package com.dayflow.domain.usecase

import com.dayflow.domain.model.*
import com.dayflow.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTaskTest {

    private fun task(
        status: TaskStatus = TaskStatus.OPEN,
        duration: Int = 90
    ) = Task(
        id = "task-1",
        title = "Study Kotlin",
        description = null,
        estimatedDurationMinutes = duration,
        priority = 2,
        energyLevel = EnergyLevel.MEDIUM,
        deadlineAtEpochMillis = null,
        status = status,
        completedAtEpochMillis = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )

    private class NoOpScheduleRepository : ScheduleRepository {
        var saved: ScheduleBlock? = null
        var savedReminder: TaskReminder? = null

        override fun observeTimeline(startAt: Long, endAt: Long): Flow<List<ScheduleBlock>> =
            emptyFlow()

        override suspend fun getBlock(id: String): ScheduleBlock? = saved?.takeIf { it.id == id }

        override suspend fun saveBlock(block: ScheduleBlock) {
            saved = block
        }

        override suspend fun saveBlockWithReminder(
            block: ScheduleBlock,
            reminder: TaskReminder
        ) {
            saved = block
            savedReminder = reminder
        }

        override suspend fun hasConflict(
            startAt: Long,
            endAt: Long,
            excludeBlockId: String?
        ): Boolean = false

        override suspend fun completeBlock(
            blockId: String,
            completedAtEpochMillis: Long
        ): ScheduleBlock = saved?.takeIf { it.id == blockId }
            ?: throw IllegalArgumentException("Schedule block not found.")

        override suspend fun applyProposal(proposalId: String) = Unit
    }

    @Test
    fun createsTaskBlockWithCorrectDurationAndLocalZone() {
        val useCase = ScheduleTask(
            repository = NoOpScheduleRepository(),
            now = { 123L },
            newId = { "block-1" }
        )

        val block = useCase.createBlock(
            task = task(duration = 90),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("09:30"),
            timeZoneId = "Asia/Manila"
        )

        assertEquals("block-1", block.id)
        assertEquals(ScheduleBlockType.TASK, block.blockType)
        assertEquals("task-1", block.taskId)
        assertNull(block.habitId)
        assertEquals("Study Kotlin", block.title)
        assertEquals("Asia/Manila", block.timeZoneId)
        assertEquals(90L * 60L * 1000L, block.endAtEpochMillis - block.startAtEpochMillis)
        assertEquals(123L, block.createdAtEpochMillis)
        assertTrue(block.isMovable)
    }

    @Test
    fun invokePersistsTheCreatedBlockThroughRepository() = runBlocking {
        val repository = NoOpScheduleRepository()
        val useCase = ScheduleTask(
            repository = repository,
            now = { 456L },
            newId = { "persisted-block" },
            newReminderId = { "reminder-1" },
            newRequestCode = { 321 }
        )

        val block = useCase(
            task = task(),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("14:00"),
            timeZoneId = "Asia/Manila"
        )

        assertEquals(block, repository.saved)
        assertEquals("reminder-1", repository.savedReminder?.id)
        assertEquals(block.id, repository.savedReminder?.scheduleBlockId)
        assertEquals(block.startAtEpochMillis, repository.savedReminder?.triggerAtEpochMillis)
        assertEquals(321, repository.savedReminder?.requestCode)
        assertEquals(ReminderStatus.SCHEDULED, repository.savedReminder?.status)
    }

    @Test
    fun invokeCanCreateTaskBlockWithoutAlarmReminder() = runBlocking {
        val repository = NoOpScheduleRepository()
        val useCase = ScheduleTask(
            repository = repository,
            now = { 456L },
            newId = { "block-without-reminder" }
        )

        val block = useCase(
            task = task(),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("14:00"),
            timeZoneId = "Asia/Manila",
            reminderEnabled = false
        )

        assertEquals(block, repository.saved)
        assertNull(repository.savedReminder)
    }

    @Test(expected = IllegalArgumentException::class)
    fun completedTasksCannotBeScheduled() {
        ScheduleTask(
            repository = NoOpScheduleRepository(),
            newId = { "block-1" }
        ).createBlock(
            task = task(status = TaskStatus.COMPLETED),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("09:00"),
            timeZoneId = "Asia/Manila"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTaskDuration() {
        ScheduleTask(
            repository = NoOpScheduleRepository(),
            newId = { "block-1" }
        ).createBlock(
            task = task(duration = 0),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("09:00"),
            timeZoneId = "Asia/Manila"
        )
    }
}
