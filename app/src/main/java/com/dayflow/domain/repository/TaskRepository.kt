package com.dayflow.domain.repository

import com.dayflow.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeActiveTasks(): Flow<List<Task>>
    fun observeUnscheduledTasks(): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun saveTask(task: Task)
    suspend fun completeTask(id: String)
    suspend fun deleteTask(id: String)
}
