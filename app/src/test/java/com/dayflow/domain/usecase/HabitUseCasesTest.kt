package com.dayflow.domain.usecase

import com.dayflow.domain.model.EnergyLevel
import com.dayflow.domain.model.FrequencyPeriod
import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.HabitProgress
import com.dayflow.domain.repository.HabitRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HabitUseCasesTest {

    private class FakeHabitRepository : HabitRepository {
        var saved: Habit? = null
        var completedDate: LocalDate? = null
        var completedTimeZone: String? = null

        override fun observeActiveHabits(): Flow<List<Habit>> = emptyFlow()
        override fun observeProgress(
            habitId: String,
            startDate: LocalDate,
            endDate: LocalDate
        ): Flow<List<HabitProgress>> = emptyFlow()
        override suspend fun getHabit(id: String): Habit? = null
        override suspend fun saveHabit(habit: Habit) { saved = habit }
        override suspend fun recordCompletion(
            habitId: String,
            localDate: LocalDate,
            timeZoneId: String
        ) {
            completedDate = localDate
            completedTimeZone = timeZoneId
        }
        override suspend fun archiveHabit(id: String) = Unit
    }

    private fun habit(
        title: String = " Read every day ",
        start: String = "2026-11-01",
        end: String? = null
    ) = Habit(
        id = "habit-1",
        title = title,
        description = null,
        frequencyPeriod = FrequencyPeriod.DAY,
        targetCount = 1,
        selectedDaysMask = 0,
        startDateIso = start,
        endDateIso = end,
        preferredStartMinuteOfDay = 480,
        durationMinutes = 20,
        energyLevel = EnergyLevel.LOW,
        reminderEnabled = false,
        isArchived = false,
        createdAtEpochMillis = 100L,
        updatedAtEpochMillis = 100L
    )

    @Test
    fun saveHabitTrimsTitleAndSetsTimestamp() = kotlinx.coroutines.runBlocking {
        val repository = FakeHabitRepository()
        SaveHabit(repository) { 250L }(habit())

        assertEquals("Read every day", repository.saved?.title)
        assertEquals(250L, repository.saved?.updatedAtEpochMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveHabitRejectsEndDateBeforeStartDate() = kotlinx.coroutines.runBlocking {
        SaveHabit(FakeHabitRepository()) { 250L }(
            habit(start = "2026-11-20", end = "2026-11-19")
        )
    }

    @Test
    fun completeHabitUsesTheUsersLocalTimeZone() = kotlinx.coroutines.runBlocking {
        val repository = FakeHabitRepository()
        val fixedClock = Clock.fixed(
            Instant.parse("2026-11-19T18:30:00Z"),
            ZoneOffset.UTC
        )

        val result = CompleteHabit(repository, fixedClock)(
            habitId = "habit-1",
            timeZoneId = "Asia/Manila"
        )

        assertEquals(LocalDate.parse("2026-11-20"), result)
        assertEquals(result, repository.completedDate)
        assertEquals("Asia/Manila", repository.completedTimeZone)
    }
}
