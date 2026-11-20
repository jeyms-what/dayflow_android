package com.dayflow.data.repository

import com.dayflow.core.database.dao.TaskDao
import com.dayflow.data.mapper.toDomain
import com.dayflow.data.mapper.toEntity
import com.dayflow.domain.model.Task
import com.dayflow.domain.model.TaskStatus
import com.dayflow.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val taskDao: TaskDao
) : TaskRepository {

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
        val now = System.currentTimeMillis()
        val updated = taskDao.updateStatus(
            taskId = id,
            status = TaskStatus.COMPLETED.name,
            completedAt = now,
            updatedAt = now
        )
        check(updated == 1) { "Task was not found or has been deleted." }
    }

    override suspend fun deleteTask(id: String) {
        val now = System.currentTimeMillis()
        val updated = taskDao.softDelete(id, deletedAt = now, updatedAt = now)
        check(updated == 1) { "Task was not found or has already been deleted." }
    }
}
