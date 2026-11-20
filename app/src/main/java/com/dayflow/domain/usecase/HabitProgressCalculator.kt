package com.dayflow.domain.usecase

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
        val monday = dateWithinWeek.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        )
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
}
