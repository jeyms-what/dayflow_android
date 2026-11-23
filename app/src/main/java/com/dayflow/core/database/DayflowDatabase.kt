package com.dayflow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dayflow.core.database.dao.*
import com.dayflow.core.database.entity.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitDailyProgressEntity::class,
        ScheduleBlockEntity::class,
        AvailabilityWindowEntity::class,
        ReminderEntity::class,
        AiScheduleProposalEntity::class,
        AiProposalBlockEntity::class,
        NoteEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class DayflowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun reminderDao(): ReminderDao
    abstract fun aiProposalDao(): AiProposalDao
    abstract fun noteDao(): NoteDao
    abstract fun workoutDao(): WorkoutDao
}


val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS notes (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                category TEXT NOT NULL,
                isPinned INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_updatedAtEpochMillis ON notes(updatedAtEpochMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isPinned ON notes(isPinned)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workout_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                startedAtEpochMillis INTEGER NOT NULL,
                endedAtEpochMillis INTEGER,
                durationSeconds INTEGER NOT NULL,
                notes TEXT NOT NULL,
                status TEXT NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_startedAtEpochMillis ON workout_sessions(startedAtEpochMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status ON workout_sessions(status)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workout_sets (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                exerciseName TEXT NOT NULL,
                muscleGroup TEXT NOT NULL,
                setNumber INTEGER NOT NULL,
                reps INTEGER NOT NULL,
                weightKg REAL NOT NULL,
                completedAtEpochMillis INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_sessionId ON workout_sets(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_exerciseName ON workout_sets(exerciseName)")
    }
}
