package com.dayflow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dayflow.core.database.dao.*
import com.dayflow.core.database.entity.*

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitDailyProgressEntity::class,
        ScheduleBlockEntity::class,
        AvailabilityWindowEntity::class,
        ReminderEntity::class,
        AiScheduleProposalEntity::class,
        AiProposalBlockEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DayflowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun reminderDao(): ReminderDao
    abstract fun aiProposalDao(): AiProposalDao
}
