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
        SELECT reminders.* FROM reminders
        INNER JOIN schedule_blocks
            ON schedule_blocks.id = reminders.scheduleBlockId
        WHERE schedule_blocks.taskId = :taskId
    """)
    suspend fun getByTaskId(taskId: String): List<ReminderEntity>

    @Query("""
        SELECT * FROM reminders
        WHERE status IN ('SCHEDULED', 'SNOOZED', 'FIRING')
        ORDER BY triggerAtEpochMillis ASC
    """)
    fun observePendingReminders(): Flow<List<ReminderEntity>>

    @Query("""
        UPDATE reminders
        SET status = 'FIRING',
            lastTriggeredAtEpochMillis = :triggeredAt,
            updatedAtEpochMillis = :triggeredAt
        WHERE id = :id
          AND status IN ('SCHEDULED', 'SNOOZED', 'FIRING')
          AND triggerAtEpochMillis <= :triggeredAt
    """)
    suspend fun markFiring(id: String, triggeredAt: Long): Int

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
