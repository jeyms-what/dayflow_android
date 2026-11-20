package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dayflow.core.database.entity.ScheduleBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Upsert
    suspend fun upsert(block: ScheduleBlockEntity)

    @Upsert
    suspend fun upsertAll(blocks: List<ScheduleBlockEntity>)

    @Query("""
        SELECT * FROM schedule_blocks
        WHERE startAtEpochMillis < :rangeEnd
          AND endAtEpochMillis > :rangeStart
          AND status NOT IN ('CANCELLED', 'SKIPPED')
        ORDER BY startAtEpochMillis ASC
    """)
    fun observeBetween(
        rangeStart: Long,
        rangeEnd: Long
    ): Flow<List<ScheduleBlockEntity>>

    @Query("SELECT * FROM schedule_blocks WHERE id = :blockId LIMIT 1")
    suspend fun getById(blockId: String): ScheduleBlockEntity?

    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM schedule_blocks
            WHERE status IN ('PLANNED', 'IN_PROGRESS')
              AND startAtEpochMillis < :endAt
              AND endAtEpochMillis > :startAt
              AND (:excludeBlockId IS NULL OR id != :excludeBlockId)
        )
    """)
    suspend fun hasOverlap(
        startAt: Long,
        endAt: Long,
        excludeBlockId: String? = null
    ): Boolean

    @Query("""
        SELECT * FROM schedule_blocks
        WHERE taskId = :taskId
        ORDER BY startAtEpochMillis ASC
    """)
    suspend fun getByTaskId(taskId: String): List<ScheduleBlockEntity>

    @Query("""
        UPDATE schedule_blocks
        SET status = :status, updatedAtEpochMillis = :updatedAt
        WHERE id = :blockId
    """)
    suspend fun updateStatus(
        blockId: String,
        status: String,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM schedule_blocks WHERE id = :blockId")
    suspend fun deleteById(blockId: String): Int
}
