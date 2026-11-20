package com.dayflow.domain.usecase

import com.dayflow.domain.model.FrequencyPeriod
import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.HabitProgress
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object HabitProgressCalculator {

    fun completedCountForDay(
        progress: List<HabitProgress>,
        date: LocalDate
    ): Int = progress
        .asSequence()
        .filter { it.localDateIso == date.toString() }
        .sumOf { it.completionCount }

    fun completedCountForWeek(
        progress: List<HabitProgress>,
        dateWithinWeek: LocalDate
    ): Int {
        val monday = weekStart(dateWithinWeek)
        val sunday = monday.plusDays(6)

        return progress.asSequence()
            .filter {
                val date = runCatching {
                    LocalDate.parse(it.localDateIso)
                }.getOrNull()
                date != null && !date.isBefore(monday) && !date.isAfter(sunday)
            }
            .sumOf { it.completionCount }
    }

    fun percentage(completed: Int, target: Int): Int {
        require(target > 0) { "Target must be greater than zero." }
        return ((completed.coerceAtLeast(0) * 100.0) / target)
            .toInt()
            .coerceAtMost(100)
    }

    fun isWeeklyTargetMet(
        progress: List<HabitProgress>,
        dateWithinWeek: LocalDate,
        targetCount: Int
    ): Boolean {
        require(targetCount > 0) { "Weekly target must be greater than zero." }
        return completedCountForWeek(progress, dateWithinWeek) >= targetCount
    }

    /**
     * Counts consecutive completed periods ending at the current period.
     *
     * A daily habit's unfinished current day does not break its streak until
     * the user has passed that day. A weekly habit's unfinished current week
     * does not break the previous streak until the week has ended.
     */
    fun currentStreak(
        habit: Habit,
        progress: List<HabitProgress>,
        today: LocalDate
    ): Int {
        require(habit.targetCount > 0) { "Habit target must be greater than zero." }
        val habitStart = LocalDate.parse(habit.startDateIso)

        return when (habit.frequencyPeriod) {
            FrequencyPeriod.DAY -> {
                var cursor = if (
                    completedCountForDay(progress, today) >= habit.targetCount
                ) today else today.minusDays(1)

                var streak = 0
                while (!cursor.isBefore(habitStart) &&
                    completedCountForDay(progress, cursor) >= habit.targetCount
                ) {
                    streak++
                    cursor = cursor.minusDays(1)
                }
                streak
            }

            FrequencyPeriod.WEEK -> {
                val currentWeekStart = weekStart(today)
                val cursorStart = if (
                    completedCountForWeek(progress, today) >= habit.targetCount
                ) currentWeekStart else currentWeekStart.minusWeeks(1)

                var cursor = cursorStart
                var streak = 0
                val firstHabitWeek = weekStart(habitStart)

                while (!cursor.isBefore(firstHabitWeek) &&
                    completedCountForWeek(progress, cursor) >= habit.targetCount
                ) {
                    streak++
                    cursor = cursor.minusWeeks(1)
                }
                streak
            }
        }
    }

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
