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
import com.dayflow.domain.model.Note
import com.dayflow.domain.model.WorkoutSession
import com.dayflow.domain.model.WorkoutSet
import com.dayflow.domain.repository.WorkspaceRepository
import com.dayflow.domain.repository.HabitRepository
import com.dayflow.domain.repository.TaskRepository
import com.dayflow.domain.repository.ScheduleRepository
import com.dayflow.domain.repository.ReminderRepository
import com.dayflow.notifications.TaskAlarmScheduler
import com.dayflow.notifications.AlarmPrecision
import com.dayflow.domain.usecase.CompleteHabit
import com.dayflow.domain.usecase.CompleteTask
import com.dayflow.domain.usecase.SaveHabit
import com.dayflow.domain.usecase.SaveTask
import com.dayflow.domain.usecase.ScheduleTask
import com.dayflow.domain.usecase.ScheduleHabit
import com.dayflow.domain.usecase.CompleteScheduleBlock
import java.time.LocalDate
import java.time.LocalTime
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
    private val habitRepository: HabitRepository,
    private val scheduleRepository: ScheduleRepository,
    private val reminderRepository: ReminderRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val workspaceRepository: WorkspaceRepository
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

    val unscheduledTasks: StateFlow<List<Task>> = taskRepository
        .observeUnscheduledTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val notes: StateFlow<List<Note>> = workspaceRepository.observeNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val workoutSessions: StateFlow<List<WorkoutSession>> = workspaceRepository
        .observeWorkoutSessions()
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
    private val scheduleTaskUseCase = ScheduleTask(scheduleRepository)
    private val scheduleHabitUseCase = ScheduleHabit(scheduleRepository)
    private val completeScheduleBlockUseCase = CompleteScheduleBlock(scheduleRepository)

    fun observeTimeline(date: LocalDate) = scheduleRepository.observeTimeline(
        startAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endAt = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    fun scheduleTask(
        taskId: String,
        date: LocalDate,
        startTime: LocalTime,
        reminderEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            runCatching {
                val task = taskRepository.getTask(taskId)
                    ?: throw IllegalArgumentException("Task no longer exists.")
                val block = scheduleTaskUseCase(
                    task = task,
                    date = date,
                    startTime = startTime,
                    timeZoneId = ZoneId.systemDefault().id,
                    reminderEnabled = reminderEnabled
                )

                if (reminderEnabled) {
                    val reminder = reminderRepository.getReminderForBlock(block.id)
                        ?: error("The task was scheduled but its reminder was not saved.")
                    when (taskAlarmScheduler.schedule(reminder)) {
                        AlarmPrecision.EXACT -> {
                            _message.value = "Task scheduled. Its alarm reminder is set."
                        }
                        AlarmPrecision.INEXACT -> {
                            _message.value =
                                "Task scheduled. Precise alarm access is off, so Android may delay the reminder."
                        }
                    }
                } else {
                    _message.value = "Task added to today's timeline without an alarm."
                }
            }.onFailure {
                _message.value = it.message ?: "Could not schedule the task."
            }
        }
    }

    fun scheduleHabit(habitId: String, date: LocalDate, startTime: LocalTime) {
        viewModelScope.launch {
            runCatching {
                val habit = habitRepository.getHabit(habitId)
                    ?: throw IllegalArgumentException("Habit no longer exists.")
                scheduleHabitUseCase(
                    habit = habit,
                    date = date,
                    startTime = startTime,
                    timeZoneId = ZoneId.systemDefault().id
                )
            }.onFailure {
                _message.value = it.message ?: "Could not schedule the habit."
            }
        }
    }

    fun completeScheduleBlock(blockId: String) {
        viewModelScope.launch {
            runCatching {
                completeScheduleBlockUseCase(blockId)
                reminderRepository.getReminderForBlock(blockId)?.let { reminder ->
                    taskAlarmScheduler.cancel(reminder)
                    taskAlarmScheduler.cancelNotification(reminder.requestCode)
                }
            }.onFailure {
                _message.value = it.message ?: "Could not complete this activity."
            }
        }
    }

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
                reminderRepository.getRemindersForTask(taskId).forEach { reminder ->
                    taskAlarmScheduler.cancel(reminder)
                    taskAlarmScheduler.cancelNotification(reminder.requestCode)
                }
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

    fun saveNote(
        id: String?,
        title: String,
        content: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val existing = id?.let { key -> notes.value.firstOrNull { it.id == key } }
                workspaceRepository.saveNote(
                    Note(
                        id = id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        content = content,
                        category = "GENERAL",
                        isPinned = isPinned,
                        createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                        updatedAtEpochMillis = now
                    )
                )
                _message.value = "Note saved."
            }.onFailure {
                _message.value = it.message ?: "Could not save the note."
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            runCatching { workspaceRepository.deleteNote(id) }
                .onFailure { _message.value = it.message ?: "Could not delete the note." }
        }
    }

    fun saveWorkout(
        title: String,
        exerciseName: String,
        muscleGroup: String,
        setCount: Int,
        reps: Int,
        weightKg: Double,
        notesText: String
    ) {
        viewModelScope.launch {
            runCatching {
                require(title.isNotBlank()) { "Workout name is required." }
                require(exerciseName.isNotBlank()) { "Exercise name is required." }
                require(setCount in 1..20) { "Sets must be between 1 and 20." }
                require(reps in 1..100) { "Reps must be between 1 and 100." }
                require(weightKg in 0.0..1000.0) { "Weight must be between 0 and 1000 kg." }
                val now = System.currentTimeMillis()
                val sessionId = UUID.randomUUID().toString()
                val sets = (1..setCount).map { setNumber ->
                    WorkoutSet(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        exerciseName = exerciseName.trim(),
                        muscleGroup = muscleGroup,
                        setNumber = setNumber,
                        reps = reps,
                        weightKg = weightKg,
                        completedAtEpochMillis = now
                    )
                }
                workspaceRepository.saveWorkoutSession(
                    session = WorkoutSession(
                        id = sessionId,
                        title = title.trim(),
                        startedAtEpochMillis = now,
                        endedAtEpochMillis = now,
                        durationSeconds = 0,
                        notes = notesText.trim(),
                        status = "COMPLETED",
                        createdAtEpochMillis = now
                    ),
                    sets = sets
                )
                _message.value = "Workout saved."
            }.onFailure {
                _message.value = it.message ?: "Could not save workout."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
