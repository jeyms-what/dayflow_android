package com.dayflow.data.repository

import javax.inject.Inject
import androidx.room.withTransaction
import com.dayflow.core.database.DayflowDatabase
import com.dayflow.core.database.dao.TaskDao
import com.dayflow.data.mapper.toDomain
import com.dayflow.data.mapper.toEntity
import com.dayflow.domain.model.Task
import com.dayflow.domain.model.TaskStatus
import com.dayflow.domain.model.ScheduleBlockStatus
import com.dayflow.domain.model.ReminderStatus
import com.dayflow.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository @Inject constructor(
    private val database: DayflowDatabase
) : TaskRepository {

    private val taskDao: TaskDao = database.taskDao()

    override fun observeActiveTasks(): Flow<List<Task>> =
        taskDao.observeActiveTasks().map { rows -> rows.map { it.toDomain() } }

    override fun observeUnscheduledTasks(): Flow<List<Task>> =
        taskDao.observeUnscheduledTasks().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTask(id: String): Task? =
        taskDao.getById(id)?.takeIf { it.deletedAtEpochMillis == null }?.toDomain()

    override suspend fun saveTask(task: Task) {
        val existing = taskDao.getById(task.id)
        require(existing?.deletedAtEpochMillis == null) {
            "A deleted task cannot be overwritten."
        }
        taskDao.upsert(
            task.toEntity(existingCreatedAt = existing?.createdAtEpochMillis ?: task.createdAtEpochMillis)
        )
    }

    override suspend fun completeTask(id: String) {
        require(id.isNotBlank()) { "Task ID cannot be blank." }

        database.withTransaction {
            val task = taskDao.getById(id)
                ?: throw IllegalArgumentException("Task was not found.")
            require(task.deletedAtEpochMillis == null) {
                "Task has been deleted."
            }
            check(task.status == TaskStatus.OPEN.name) {
                "Only open tasks can be completed."
            }

            val now = System.currentTimeMillis()
            val updated = taskDao.updateStatus(
                taskId = id,
                status = TaskStatus.COMPLETED.name,
                completedAt = now,
                updatedAt = now
            )
            check(updated == 1) { "Task could not be completed." }

            // Keep all linked timeline blocks and reminder records consistent
            // when the task is completed from Today rather than its timeline card.
            database.scheduleDao().getByTaskId(id).forEach { block ->
                if (block.status == ScheduleBlockStatus.PLANNED.name ||
                    block.status == ScheduleBlockStatus.IN_PROGRESS.name
                ) {
                    val blockUpdated = database.scheduleDao().updateStatus(
                        blockId = block.id,
                        status = ScheduleBlockStatus.COMPLETED.name,
                        updatedAt = now
                    )
                    check(blockUpdated == 1) {
                        "A linked schedule block could not be completed."
                    }

                    database.reminderDao().getByBlockId(block.id)?.let { reminder ->
                        if (reminder.status in listOf(
                                ReminderStatus.SCHEDULED.name,
                                ReminderStatus.SNOOZED.name,
                                ReminderStatus.FIRING.name
                            )
                        ) {
                            val reminderUpdated = database.reminderDao().updateReminderState(
                                id = reminder.id,
                                status = ReminderStatus.COMPLETED.name,
                                triggerAt = reminder.triggerAtEpochMillis,
                                snoozeUntil = null,
                                updatedAt = now
                            )
                            check(reminderUpdated == 1) {
                                "A linked reminder could not be completed."
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun deleteTask(id: String) {
        val now = System.currentTimeMillis()
        val updated = taskDao.softDelete(id, deletedAt = now, updatedAt = now)
        check(updated == 1) { "Task was not found or has already been deleted." }
    }
}
