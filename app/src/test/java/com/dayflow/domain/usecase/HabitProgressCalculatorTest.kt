package com.dayflow.domain.usecase

import com.dayflow.domain.model.FrequencyPeriod
import com.dayflow.domain.model.HabitProgress
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitProgressCalculatorTest {

    private fun progress(date: String, count: Int) = HabitProgress(
        habitId = "exercise",
        localDateIso = date,
        timeZoneId = "Asia/Manila",
        completionCount = count,
        targetCountAtTime = 3,
        frequencyPeriodAtTime = FrequencyPeriod.WEEK,
        updatedAtEpochMillis = 0L
    )

    @Test
    fun weeklyCountIncludesMondayThroughSundayOnly() {
        val rows = listOf(
            progress("2026-11-16", 1), // Monday
            progress("2026-11-18", 1), // Wednesday
            progress("2026-11-22", 1), // Sunday
            progress("2026-11-23", 5)  // Following Monday
        )

        assertEquals(
            3,
            HabitProgressCalculator.completedCountForWeek(
                rows,
                LocalDate.parse("2026-11-19")
            )
        )
    }

    @Test
    fun weeklyTargetIsMetAtOrAboveTarget() {
        val rows = listOf(
            progress("2026-11-16", 1),
            progress("2026-11-18", 2)
        )

        assertTrue(
            HabitProgressCalculator.isWeeklyTargetMet(
                rows,
                LocalDate.parse("2026-11-20"),
                targetCount = 3
            )
        )
        assertFalse(
            HabitProgressCalculator.isWeeklyTargetMet(
                rows,
                LocalDate.parse("2026-11-20"),
                targetCount = 4
            )
        )
    }

    @Test
    fun percentageIsBoundedBetweenZeroAndOneHundred() {
        assertEquals(0, HabitProgressCalculator.percentage(-1, 3))
        assertEquals(66, HabitProgressCalculator.percentage(2, 3))
        assertEquals(100, HabitProgressCalculator.percentage(8, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun percentageRejectsZeroTarget() {
        HabitProgressCalculator.percentage(1, 0)
    }
}
