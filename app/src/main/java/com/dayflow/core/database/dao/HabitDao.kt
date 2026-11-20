package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dayflow.core.database.entity.HabitDailyProgressEntity
import com.dayflow.core.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("""
        SELECT * FROM habits
        WHERE isArchived = 0
        ORDER BY createdAtEpochMillis DESC
    """)
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getById(habitId: String): HabitEntity?

    @Query("""
        UPDATE habits
        SET isArchived = 1, updatedAtEpochMillis = :updatedAt
        WHERE id = :habitId AND isArchived = 0
    """)
    suspend fun archiveHabit(habitId: String, updatedAt: Long): Int

    @Upsert
    suspend fun upsertProgress(progress: HabitDailyProgressEntity)

    @Query("""
        SELECT * FROM habit_daily_progress
        WHERE habitId = :habitId AND localDateIso = :date
        LIMIT 1
    """)
    suspend fun getDailyProgress(
        habitId: String,
        date: String
    ): HabitDailyProgressEntity?

    @Query("""
        SELECT * FROM habit_daily_progress
        WHERE habitId = :habitId
          AND localDateIso BETWEEN :startDate AND :endDate
        ORDER BY localDateIso ASC
    """)
    fun observeProgress(
        habitId: String,
        startDate: String,
        endDate: String
    ): Flow<List<HabitDailyProgressEntity>>

    @Transaction
    suspend fun incrementDailyProgress(
        habitId: String,
        localDateIso: String,
        timeZoneId: String,
        targetCount: Int,
        frequencyPeriod: String,
        updatedAt: Long
    ) {
        val current = getDailyProgress(habitId, localDateIso)
        val updated = if (current == null) {
            HabitDailyProgressEntity(
                habitId = habitId,
                localDateIso = localDateIso,
                timeZoneId = timeZoneId,
                completionCount = 1,
                targetCountAtTime = targetCount,
                frequencyPeriodAtTime = frequencyPeriod,
                updatedAtEpochMillis = updatedAt
            )
        } else {
            current.copy(
                completionCount = current.completionCount + 1,
                updatedAtEpochMillis = updatedAt
            )
        }
        upsertProgress(updated)
    }
}
