package com.dayflow.di

import com.dayflow.notifications.AndroidTaskAlarmScheduler
import com.dayflow.notifications.TaskAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {
    @Binds
    @Singleton
    abstract fun bindTaskAlarmScheduler(
        implementation: AndroidTaskAlarmScheduler
    ): TaskAlarmScheduler
}
