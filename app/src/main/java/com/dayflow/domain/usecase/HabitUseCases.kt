package com.dayflow.domain.usecase

import com.dayflow.domain.model.Habit
import com.dayflow.domain.repository.HabitRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class SaveHabit(
    private val repository: HabitRepository,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(habit: Habit) {
        require(habit.title.isNotBlank()) { "Habit title cannot be blank." }
        require(habit.targetCount > 0) { "Habit target must be greater than zero." }
        require(habit.durationMinutes > 0) { "Habit duration must be greater than zero." }
        require(habit.selectedDaysMask in 0..127) {
            "Selected days mask must only use the seven weekday bits."
        }
        require(habit.preferredStartMinuteOfDay == null ||
            habit.preferredStartMinuteOfDay in 0..1439) {
            "Preferred time must be between 00:00 and 23:59."
        }

        val startDate = LocalDate.parse(habit.startDateIso)
        val endDate = habit.endDateIso?.let(LocalDate::parse)
        require(endDate == null || !endDate.isBefore(startDate)) {
            "Habit end date cannot be before its start date."
        }

        repository.saveHabit(
            habit.copy(
                title = habit.title.trim(),
                updatedAtEpochMillis = now()
            )
        )
    }
}

class CompleteHabit(
    private val repository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend operator fun invoke(
        habitId: String,
        timeZoneId: String
    ): LocalDate {
        require(habitId.isNotBlank()) { "Habit ID cannot be blank." }
        val zone = ZoneId.of(timeZoneId)
        val localDate = LocalDate.now(clock.withZone(zone))
        repository.recordCompletion(
            habitId = habitId,
            localDate = localDate,
            timeZoneId = timeZoneId
        )
        return localDate
    }
}

class ArchiveHabit(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) {
        require(habitId.isNotBlank()) { "Habit ID cannot be blank." }
        repository.archiveHabit(habitId)
    }
}
