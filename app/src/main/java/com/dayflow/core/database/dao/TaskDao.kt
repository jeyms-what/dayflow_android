package com.dayflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dayflow.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("""
        SELECT * FROM tasks
        WHERE deletedAtEpochMillis IS NULL
          AND status NOT IN ('COMPLETED', 'CANCELLED')
        ORDER BY priority DESC, deadlineAtEpochMillis ASC
    """)
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: String): TaskEntity?

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'OPEN'
          AND deletedAtEpochMillis IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM schedule_blocks
              WHERE schedule_blocks.taskId = tasks.id
                AND schedule_blocks.status IN ('PLANNED', 'IN_PROGRESS')
          )
        ORDER BY priority DESC, deadlineAtEpochMillis ASC
    """)
    fun observeUnscheduledTasks(): Flow<List<TaskEntity>>

    @Query("""
        UPDATE tasks
        SET status = :status,
            completedAtEpochMillis = :completedAt,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :taskId
          AND deletedAtEpochMillis IS NULL
    """)
    suspend fun updateStatus(
        taskId: String,
        status: String,
        completedAt: Long?,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE tasks
        SET deletedAtEpochMillis = :deletedAt,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :taskId
          AND deletedAtEpochMillis IS NULL
    """)
    suspend fun softDelete(
        taskId: String,
        deletedAt: Long,
        updatedAt: Long
    ): Int
}
