package com.dayflow.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val isPinned: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class WorkoutSession(
    val id: String,
    val title: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val durationSeconds: Long,
    val notes: String,
    val status: String,
    val createdAtEpochMillis: Long
)

data class WorkoutSet(
    val id: String,
    val sessionId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val completedAtEpochMillis: Long
)


data class WorkoutSetInput(
    val reps: Int,
    val weightKg: Double
)

data class WorkoutExerciseInput(
    val exerciseName: String,
    val muscleGroup: String,
    val sets: List<WorkoutSetInput>
)
