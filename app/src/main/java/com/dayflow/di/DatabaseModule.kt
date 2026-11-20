package com.dayflow.di

import android.content.Context
import androidx.room.Room
import com.dayflow.core.database.DayflowDatabase
import com.dayflow.core.database.dao.*
import com.dayflow.data.repository.*
import com.dayflow.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDayflowDatabase(
        @ApplicationContext context: Context
    ): DayflowDatabase =
        Room.databaseBuilder(
            context,
            DayflowDatabase::class.java,
            "dayflow_database"
        ).build()

    @Provides fun provideTaskDao(database: DayflowDatabase): TaskDao = database.taskDao()
    @Provides fun provideHabitDao(database: DayflowDatabase): HabitDao = database.habitDao()
    @Provides fun provideScheduleDao(database: DayflowDatabase): ScheduleDao = database.scheduleDao()
    @Provides fun provideAvailabilityDao(database: DayflowDatabase): AvailabilityDao = database.availabilityDao()
    @Provides fun provideReminderDao(database: DayflowDatabase): ReminderDao = database.reminderDao()
    @Provides fun provideAiProposalDao(database: DayflowDatabase): AiProposalDao = database.aiProposalDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindTaskRepository(implementation: RoomTaskRepository): TaskRepository

    @Binds @Singleton
    abstract fun bindHabitRepository(implementation: RoomHabitRepository): HabitRepository

    @Binds @Singleton
    abstract fun bindScheduleRepository(implementation: RoomScheduleRepository): ScheduleRepository

    @Binds @Singleton
    abstract fun bindReminderRepository(implementation: RoomReminderRepository): ReminderRepository

    @Binds @Singleton
    abstract fun bindAiProposalRepository(implementation: RoomAiProposalRepository): AiProposalRepository
}
