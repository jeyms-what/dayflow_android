package com.dayflow.domain.usecase

import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.ScheduleBlock
import com.dayflow.domain.model.ScheduleBlockStatus
import com.dayflow.domain.model.ScheduleBlockType
import com.dayflow.domain.model.ScheduleSource
import com.dayflow.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class ScheduleHabit(
    private val repository: ScheduleRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    fun createBlock(
        habit: Habit,
        date: LocalDate,
        startTime: LocalTime,
        timeZoneId: String
    ): ScheduleBlock {
        require(!habit.isArchived) { "Archived habits cannot be scheduled." }
        require(habit.id.isNotBlank()) { "Habit ID cannot be blank." }
        require(habit.title.isNotBlank()) { "Habit title cannot be blank." }
        require(habit.durationMinutes in 1..1440) {
            "Habit duration must be between 1 and 1440 minutes."
        }

        val startDate = LocalDate.parse(habit.startDateIso)
        val endDate = habit.endDateIso?.let(LocalDate::parse)
        require(!date.isBefore(startDate)) { "Habit is not active on this date yet." }
        require(endDate == null || !date.isAfter(endDate)) {
            "Habit is no longer active on this date."
        }

        val dayBit = 1 shl (date.dayOfWeek.value - 1)
        require(habit.selectedDaysMask == 0 || (habit.selectedDaysMask and dayBit) != 0) {
            "This habit is not scheduled for the selected weekday."
        }

        val zone = ZoneId.of(timeZoneId)
        val localStart = date.atTime(startTime)
        val start = localStart.atZone(zone).toInstant().toEpochMilli()
        val end = localStart
            .plusMinutes(habit.durationMinutes.toLong())
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        require(end > start) { "Scheduled habit must have a positive duration." }

        val timestamp = now()
        return ScheduleBlock(
            id = newId(),
            blockType = ScheduleBlockType.HABIT,
            taskId = null,
            habitId = habit.id,
            title = habit.title,
            startAtEpochMillis = start,
            endAtEpochMillis = end,
            timeZoneId = zone.id,
            status = ScheduleBlockStatus.PLANNED,
            source = ScheduleSource.MANUAL,
            isMovable = true,
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp
        )
    }

    suspend operator fun invoke(
        habit: Habit,
        date: LocalDate,
        startTime: LocalTime,
        timeZoneId: String
    ): ScheduleBlock {
        val block = createBlock(habit, date, startTime, timeZoneId)
        repository.saveBlock(block)
        return block
    }
}
