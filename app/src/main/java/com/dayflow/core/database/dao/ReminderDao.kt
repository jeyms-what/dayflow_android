package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dayflow.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("""
        SELECT * FROM reminders
        WHERE scheduleBlockId = :blockId
        LIMIT 1
    """)
    suspend fun getByBlockId(blockId: String): ReminderEntity?

    @Query("""
        SELECT * FROM reminders
        WHERE status IN ('SCHEDULED', 'SNOOZED')
        ORDER BY triggerAtEpochMillis ASC
    """)
    fun observePendingReminders(): Flow<List<ReminderEntity>>

    @Query("""
        SELECT * FROM reminders
        WHERE status IN ('SCHEDULED', 'SNOOZED')
          AND triggerAtEpochMillis <= :now
    """)
    suspend fun getDueReminders(now: Long): List<ReminderEntity>

    @Query("""
        UPDATE reminders
        SET status = :status,
            triggerAtEpochMillis = :triggerAt,
            snoozeUntilEpochMillis = :snoozeUntil,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateReminderState(
        id: String,
        status: String,
        triggerAt: Long,
        snoozeUntil: Long?,
        updatedAt: Long
    ): Int
}
