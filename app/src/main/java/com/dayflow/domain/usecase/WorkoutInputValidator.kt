package com.dayflow.domain.usecase

import com.dayflow.domain.model.WorkoutExerciseInput

object WorkoutInputValidator {
    fun validate(title: String, durationMinutes: Int, exercises: List<WorkoutExerciseInput>): String? {
        if (title.isBlank()) return "Workout name is required."
        if (durationMinutes !in 1..600) return "Workout duration must be between 1 and 600 minutes.\n"
        if (exercises.isEmpty()) return "Add at least one exercise."
        if (exercises.size > 20) return "A workout can contain up to 20 exercises."

        exercises.forEachIndexed { index, exercise ->
            if (exercise.exerciseName.isBlank()) return "Exercise ${index + 1} needs a name."
            if (exercise.muscleGroup.isBlank()) return "Choose a muscle group for exercise ${index + 1}."
            if (exercise.sets.isEmpty()) return "Add at least one set to exercise ${index + 1}."
            if (exercise.sets.size > 20) return "Each exercise can contain up to 20 sets."
            if (exercise.sets.any { it.reps !in 1..100 }) {
                return "Reps must be between 1 and 100."
            }
            if (exercise.sets.any { !it.weightKg.isFinite() || it.weightKg !in 0.0..1000.0 }) {
                return "Weight must be between 0 and 1000 kg."
            }
        }
        return null
    }
}
