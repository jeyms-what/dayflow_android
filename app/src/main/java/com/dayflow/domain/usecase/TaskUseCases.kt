package com.dayflow.domain.usecase

import com.dayflow.domain.model.Task
import com.dayflow.domain.repository.TaskRepository
import java.util.UUID

class SaveTask(
    private val repository: TaskRepository,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(task: Task) {
        require(task.title.isNotBlank()) { "Task title cannot be blank." }
        require(task.estimatedDurationMinutes > 0) {
            "Task duration must be greater than zero."
        }
        require(task.priority in 1..4) { "Task priority must be between 1 and 4." }
        require(task.deadlineAtEpochMillis == null || task.deadlineAtEpochMillis > 0L) {
            "Task deadline must be a valid epoch timestamp."
        }
        val timestamp = now()
        repository.saveTask(
            task.copy(
                title = task.title.trim(),
                updatedAtEpochMillis = timestamp
            )
        )
    }
}

class CompleteTask(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) {
        require(taskId.isNotBlank()) { "Task ID cannot be blank." }
        repository.completeTask(taskId)
    }
}

fun newTaskId(): String = UUID.randomUUID().toString()
