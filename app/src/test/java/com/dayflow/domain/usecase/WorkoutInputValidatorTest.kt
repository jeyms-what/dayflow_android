package com.dayflow.domain.usecase

import com.dayflow.domain.model.WorkoutExerciseInput
import com.dayflow.domain.model.WorkoutSetInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutInputValidatorTest {

    @Test
    fun acceptsMultipleExercisesWithIndividualSetValues() {
        val exercises = listOf(
            WorkoutExerciseInput(
                exerciseName = "Bench Press",
                muscleGroup = "Chest",
                sets = listOf(
                    WorkoutSetInput(reps = 10, weightKg = 40.0),
                    WorkoutSetInput(reps = 8, weightKg = 45.0),
                    WorkoutSetInput(reps = 6, weightKg = 50.0)
                )
            ),
            WorkoutExerciseInput(
                exerciseName = "Incline Dumbbell Press",
                muscleGroup = "Chest",
                sets = listOf(WorkoutSetInput(reps = 12, weightKg = 15.0))
            )
        )

        assertNull(WorkoutInputValidator.validate("Push day", exercises))
    }

    @Test
    fun rejectsEmptyWorkout() {
        assertEquals(
            "Add at least one exercise.",
            WorkoutInputValidator.validate("Push day", emptyList())
        )
    }

    @Test
    fun rejectsSetsWithInvalidRepCount() {
        val exercises = listOf(
            WorkoutExerciseInput(
                exerciseName = "Squat",
                muscleGroup = "Legs",
                sets = listOf(WorkoutSetInput(reps = 0, weightKg = 60.0))
            )
        )

        assertEquals(
            "Reps must be between 1 and 100.",
            WorkoutInputValidator.validate("Leg day", exercises)
        )
    }

    @Test
    fun rejectsNonFiniteWeight() {
        val exercises = listOf(
            WorkoutExerciseInput(
                exerciseName = "Deadlift",
                muscleGroup = "Back",
                sets = listOf(WorkoutSetInput(reps = 5, weightKg = Double.NaN))
            )
        )

        assertEquals(
            "Weight must be between 0 and 1000 kg.",
            WorkoutInputValidator.validate("Pull day", exercises)
        )
    }
}
