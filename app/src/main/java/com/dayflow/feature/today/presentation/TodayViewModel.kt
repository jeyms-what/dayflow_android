package com.dayflow.feature.today.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.dayflow.domain.model.EnergyLevel
import com.dayflow.domain.model.FrequencyPeriod
import com.dayflow.domain.model.Habit
import com.dayflow.domain.model.Task
import com.dayflow.domain.model.TaskStatus
import com.dayflow.domain.repository.HabitRepository
import com.dayflow.domain.repository.TaskRepository
import com.dayflow.domain.usecase.CompleteHabit
import com.dayflow.domain.usecase.CompleteTask
import com.dayflow.domain.usecase.SaveHabit
import com.dayflow.domain.usecase.SaveTask
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository
        .observeActiveTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val habits: StateFlow<List<Habit>> = habitRepository
        .observeActiveHabits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val saveTask = SaveTask(taskRepository)
    private val completeTaskUseCase = CompleteTask(taskRepository)
    private val saveHabit = SaveHabit(habitRepository)
    private val completeHabitUseCase = CompleteHabit(habitRepository)

    fun observeProgress(habit: Habit) = habitRepository.observeProgress(
        habitId = habit.id,
        startDate = LocalDate.parse(habit.startDateIso),
        endDate = LocalDate.now(ZoneId.systemDefault())
    )

    fun addTask(title: String, durationMinutes: Int) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                saveTask(
                    Task(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        description = null,
                        estimatedDurationMinutes = durationMinutes,
                        priority = 2,
                        energyLevel = EnergyLevel.MEDIUM,
                        deadlineAtEpochMillis = null,
                        status = TaskStatus.OPEN,
                        completedAtEpochMillis = null,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now
                    )
                )
            }.onFailure {
                _message.value = it.message ?: "Could not save the task."
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                completeTaskUseCase(taskId)
            }.onFailure {
                _message.value = it.message ?: "Could not complete the task."
            }
        }
    }

    fun addHabit(
        title: String,
        frequencyPeriod: FrequencyPeriod,
        targetCount: Int
    ) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val zone = ZoneId.systemDefault()
                saveHabit(
                    Habit(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        description = null,
                        frequencyPeriod = frequencyPeriod,
                        targetCount = targetCount,
                        selectedDaysMask = 0,
                        startDateIso = LocalDate.now(zone).toString(),
                        endDateIso = null,
                        preferredStartMinuteOfDay = null,
                        durationMinutes = 15,
                        energyLevel = EnergyLevel.MEDIUM,
                        reminderEnabled = false,
                        isArchived = false,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now
                    )
                )
            }.onFailure {
                _message.value = it.message ?: "Could not save the habit."
            }
        }
    }

    fun completeHabit(habitId: String) {
        viewModelScope.launch {
            runCatching {
                completeHabitUseCase(
                    habitId = habitId,
                    timeZoneId = ZoneId.systemDefault().id
                )
            }.onFailure {
                _message.value = it.message ?: "Could not log the habit."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
