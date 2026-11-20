package com.dayflow.domain.usecase

import com.dayflow.domain.model.EnergyLevel
import com.dayflow.domain.model.FrequencyPeriod
import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.HabitProgress
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitStreakTest {

    private fun habit(period: FrequencyPeriod, target: Int) = Habit(
        id = "habit-1",
        title = "Exercise",
        description = null,
        frequencyPeriod = period,
        targetCount = target,
        selectedDaysMask = 0,
        startDateIso = "2026-10-01",
        endDateIso = null,
        preferredStartMinuteOfDay = null,
        durationMinutes = 30,
        energyLevel = EnergyLevel.HIGH,
        reminderEnabled = false,
        isArchived = false,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    private fun row(date: String, count: Int, period: FrequencyPeriod) =
        HabitProgress(
            habitId = "habit-1",
            localDateIso = date,
            timeZoneId = "Asia/Manila",
            completionCount = count,
            targetCountAtTime = 1,
            frequencyPeriodAtTime = period,
            updatedAtEpochMillis = 0L
        )

    @Test
    fun dailyStreakStartsFromYesterdayWhenTodayIsStillPending() {
        val rows = listOf(
            row("2026-11-17", 1, FrequencyPeriod.DAY),
            row("2026-11-18", 1, FrequencyPeriod.DAY),
            row("2026-11-19", 1, FrequencyPeriod.DAY)
        )

        assertEquals(
            3,
            HabitProgressCalculator.currentStreak(
                habit(FrequencyPeriod.DAY, 1),
                rows,
                LocalDate.parse("2026-11-20")
            )
        )
    }

    @Test
    fun weeklyStreakDoesNotBreakDuringAnIncompleteCurrentWeek() {
        val rows = listOf(
            row("2026-11-02", 3, FrequencyPeriod.WEEK),
            row("2026-11-09", 3, FrequencyPeriod.WEEK),
            row("2026-11-16", 2, FrequencyPeriod.WEEK)
        )

        assertEquals(
            2,
            HabitProgressCalculator.currentStreak(
                habit(FrequencyPeriod.WEEK, 3),
                rows,
                LocalDate.parse("2026-11-19")
            )
        )
    }

    @Test
    fun weeklyStreakIncludesCurrentWeekWhenTargetIsMet() {
        val rows = listOf(
            row("2026-11-02", 3, FrequencyPeriod.WEEK),
            row("2026-11-09", 3, FrequencyPeriod.WEEK),
            row("2026-11-16", 3, FrequencyPeriod.WEEK)
        )

        assertEquals(
            3,
            HabitProgressCalculator.currentStreak(
                habit(FrequencyPeriod.WEEK, 3),
                rows,
                LocalDate.parse("2026-11-19")
            )
        )
    }
}
