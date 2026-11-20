package com.dayflow.domain.usecase

import com.dayflow.domain.model.EnergyLevel
import com.dayflow.domain.model.Task
import com.dayflow.domain.model.TaskStatus
import com.dayflow.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SaveTaskTest {

    private class FakeTaskRepository : TaskRepository {
        var saved: Task? = null
        override fun observeActiveTasks(): Flow<List<Task>> = emptyFlow()
        override fun observeUnscheduledTasks(): Flow<List<Task>> = emptyFlow()
        override suspend fun getTask(id: String): Task? = null
        override suspend fun saveTask(task: Task) { saved = task }
        override suspend fun completeTask(id: String) = Unit
        override suspend fun deleteTask(id: String) = Unit
    }

    private fun task(
        title: String = "  Study Kotlin  ",
        duration: Int = 60,
        priority: Int = 2
    ) = Task(
        id = "task-1",
        title = title,
        description = null,
        estimatedDurationMinutes = duration,
        priority = priority,
        energyLevel = EnergyLevel.HIGH,
        deadlineAtEpochMillis = null,
        status = TaskStatus.OPEN,
        completedAtEpochMillis = null,
        createdAtEpochMillis = 100L,
        updatedAtEpochMillis = 100L
    )

    @Test
    fun saveTrimsTitleAndSetsUpdateTimestamp() = kotlinx.coroutines.runBlocking {
        val repository = FakeTaskRepository()
        val saveTask = SaveTask(repository) { 200L }

        saveTask(task())

        val saved = repository.saved
        assertNotNull(saved)
        assertEquals("Study Kotlin", saved?.title)
        assertEquals(200L, saved?.updatedAtEpochMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveRejectsBlankTitle() = kotlinx.coroutines.runBlocking {
        SaveTask(FakeTaskRepository()) { 200L }(task(title = "   "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveRejectsZeroDuration() = kotlinx.coroutines.runBlocking {
        SaveTask(FakeTaskRepository()) { 200L }(task(duration = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveRejectsInvalidPriority() = kotlinx.coroutines.runBlocking {
        SaveTask(FakeTaskRepository()) { 200L }(task(priority = 5))
    }
}
