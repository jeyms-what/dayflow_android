package com.dayflow

import android.content.Context
import com.dayflow.core.database.DatabaseProvider
import com.dayflow.data.repository.RoomAiProposalRepository
import com.dayflow.data.repository.RoomHabitRepository
import com.dayflow.data.repository.RoomReminderRepository
import com.dayflow.data.repository.RoomScheduleRepository
import com.dayflow.data.repository.RoomTaskRepository

/**
 * Lightweight composition root for the first implementation milestone.
 *
 * This keeps Android construction out of domain code. We can migrate this
 * container to Hilt after the first local data workflows are stable.
 */
class DayflowContainer(context: Context) {
    val database = DatabaseProvider.getDatabase(context)

    val taskRepository = RoomTaskRepository(database.taskDao())
    val habitRepository = RoomHabitRepository(database.habitDao())
    val scheduleRepository = RoomScheduleRepository(database)
    val reminderRepository = RoomReminderRepository(database)
    val aiProposalRepository = RoomAiProposalRepository(database.aiProposalDao())
}
