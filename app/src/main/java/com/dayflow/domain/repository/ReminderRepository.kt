package com.dayflow.domain.repository

import com.dayflow.domain.model.TaskReminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observePendingReminders(): Flow<List<TaskReminder>>
    suspend fun saveReminder(reminder: TaskReminder)
    suspend fun snoozeReminder(reminderId: String, newTriggerAt: Long)
    suspend fun dismissReminder(reminderId: String)
    suspend fun completeReminder(reminderId: String)
}
