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

class ScheduleHabitTest {

    private fun habit(
        archived: Boolean = false,
        selectedDaysMask: Int = 0,
        duration: Int = 20
    ) = Habit(
        id = "habit-1",
        title = "Exercise",
        description = null,
        frequencyPeriod = FrequencyPeriod.WEEK,
        targetCount = 3,
        selectedDaysMask = selectedDaysMask,
        startDateIso = "2026-11-01",
        endDateIso = null,
        preferredStartMinuteOfDay = null,
        durationMinutes = duration,
        energyLevel = EnergyLevel.MEDIUM,
        reminderEnabled = false,
        isArchived = archived,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )

    private class FakeScheduleRepository : ScheduleRepository {
        var saved: ScheduleBlock? = null

        override fun observeTimeline(startAt: Long, endAt: Long): Flow<List<ScheduleBlock>> =
            emptyFlow()

        override suspend fun getBlock(id: String): ScheduleBlock? =
            saved?.takeIf { it.id == id }

        override suspend fun saveBlock(block: ScheduleBlock) {
            saved = block
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
    fun createsHabitBlockUsingHabitDurationAndCorrectReference() {
        val repository = FakeScheduleRepository()
        val useCase = ScheduleHabit(
            repository = repository,
            now = { 123L },
            newId = { "habit-block-1" }
        )

        val block = useCase.createBlock(
            habit = habit(duration = 20),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("07:30"),
            timeZoneId = "Asia/Manila"
        )

        assertEquals("habit-block-1", block.id)
        assertEquals(ScheduleBlockType.HABIT, block.blockType)
        assertNull(block.taskId)
        assertEquals("habit-1", block.habitId)
        assertEquals("Exercise", block.title)
        assertEquals("Asia/Manila", block.timeZoneId)
        assertEquals(20L * 60L * 1000L, block.endAtEpochMillis - block.startAtEpochMillis)
        assertEquals(123L, block.createdAtEpochMillis)
        assertTrue(block.isMovable)
    }

    @Test
    fun invokePersistsHabitBlockThroughRepository() = runBlocking {
        val repository = FakeScheduleRepository()
        val useCase = ScheduleHabit(
            repository = repository,
            now = { 456L },
            newId = { "saved-habit-block" }
        )

        val block = useCase(
            habit = habit(),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("08:00"),
            timeZoneId = "Asia/Manila"
        )

        assertEquals(block, repository.saved)
    }

    @Test(expected = IllegalArgumentException::class)
    fun archivedHabitsCannotBeScheduled() {
        ScheduleHabit(
            repository = FakeScheduleRepository()
        ).createBlock(
            habit = habit(archived = true),
            date = LocalDate.parse("2026-11-20"),
            startTime = LocalTime.parse("08:00"),
            timeZoneId = "Asia/Manila"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDaysNotSelectedForHabit() {
        val fridayBit = 1 shl 4
        ScheduleHabit(
            repository = FakeScheduleRepository()
        ).createBlock(
            habit = habit(selectedDaysMask = fridayBit),
            date = LocalDate.parse("2026-11-23"), // Monday
            startTime = LocalTime.parse("08:00"),
            timeZoneId = "Asia/Manila"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDatesBeforeHabitStartDate() {
        ScheduleHabit(
            repository = FakeScheduleRepository()
        ).createBlock(
            habit = habit(),
            date = LocalDate.parse("2026-10-31"),
            startTime = LocalTime.parse("08:00"),
            timeZoneId = "Asia/Manila"
        )
    }
}
