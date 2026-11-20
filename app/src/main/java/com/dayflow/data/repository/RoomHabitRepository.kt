package com.dayflow.data.repository

import javax.inject.Inject
import com.dayflow.core.database.dao.HabitDao
import com.dayflow.data.mapper.toDomain
import com.dayflow.data.mapper.toEntity
import com.dayflow.domain.model.Habit
import com.dayflow.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHabitRepository @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActiveHabits().map { rows -> rows.map { it.toDomain() } }

    override fun observeProgress(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ) = habitDao.observeProgress(
        habitId = habitId,
        startDate = startDate.toString(),
        endDate = endDate.toString()
    ).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getHabit(id: String): Habit? =
        habitDao.getById(id)?.toDomain()

    override suspend fun saveHabit(habit: Habit) {
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
        require(
            habit.endDateIso == null ||
                !LocalDate.parse(habit.endDateIso).isBefore(LocalDate.parse(habit.startDateIso))
        ) { "Habit end date cannot be before its start date." }

        val existing = habitDao.getById(habit.id)
        val now = System.currentTimeMillis()
        habitDao.upsert(
            habit.copy(
                title = habit.title.trim(),
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: habit.createdAtEpochMillis,
                updatedAtEpochMillis = now
            ).toEntity(
                existingCreatedAt = existing?.createdAtEpochMillis ?: habit.createdAtEpochMillis
            )
        )
    }

    override suspend fun recordCompletion(
        habitId: String,
        localDate: LocalDate,
        timeZoneId: String
    ) {
        val habit = habitDao.getById(habitId)
            ?: throw IllegalArgumentException("Habit was not found.")

        require(!habit.isArchived) { "Archived habits cannot be completed." }
        require(!localDate.isBefore(LocalDate.parse(habit.startDateIso))) {
            "Habit has not started yet."
        }
        require(habit.endDateIso == null ||
            !localDate.isAfter(LocalDate.parse(habit.endDateIso))) {
            "Habit has already ended."
        }

        if (habit.selectedDaysMask != 0) {
            val dayBit = 1 shl (localDate.dayOfWeek.value - 1)
            require(habit.selectedDaysMask and dayBit != 0) {
                "This habit is not scheduled for the selected weekday."
            }
        }

        // Validate the zone before writing so malformed IDs cannot corrupt date semantics.
        java.time.ZoneId.of(timeZoneId)

        habitDao.incrementDailyProgress(
            habitId = habitId,
            localDateIso = localDate.toString(),
            timeZoneId = timeZoneId,
            targetCount = habit.targetCount,
            frequencyPeriod = habit.frequencyPeriod,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun archiveHabit(id: String) {
        val updated = habitDao.archiveHabit(id, System.currentTimeMillis())
        check(updated == 1) { "Habit was not found or is already archived." }
    }
}
