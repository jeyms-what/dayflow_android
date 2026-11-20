package com.dayflow.domain.repository

import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.HabitProgress
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeProgress(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitProgress>>
    suspend fun getHabit(id: String): Habit?
    suspend fun saveHabit(habit: Habit)
    suspend fun recordCompletion(
        habitId: String,
        localDate: LocalDate,
        timeZoneId: String
    )
    suspend fun archiveHabit(id: String)
}
