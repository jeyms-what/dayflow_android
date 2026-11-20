package com.dayflow.notifications

import com.dayflow.domain.model.TaskReminder

enum class AlarmPrecision {
    EXACT,
    INEXACT
}

/** Registers and cancels device-local task reminders. */
interface TaskAlarmScheduler {
    fun schedule(reminder: TaskReminder): AlarmPrecision
    fun cancel(reminder: TaskReminder)
    fun cancelNotification(requestCode: Int)
}
