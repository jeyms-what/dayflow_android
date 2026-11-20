package com.dayflow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dayflow.domain.model.ReminderStatus
import com.dayflow.domain.repository.ReminderRepository
import com.dayflow.domain.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var scheduleRepository: ScheduleRepository
    @Inject lateinit var alarmScheduler: TaskAlarmScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FIRE) return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = reminderRepository.getReminder(reminderId) ?: return@launch
                if (reminder.status !in listOf(
                        ReminderStatus.SCHEDULED,
                        ReminderStatus.SNOOZED,
                        ReminderStatus.FIRING
                    )
                ) return@launch

                val now = System.currentTimeMillis()
                if (reminder.triggerAtEpochMillis > now + EARLY_TOLERANCE_MILLIS) {
                    // The system clock changed after this PendingIntent was registered.
                    alarmScheduler.schedule(reminder)
                    return@launch
                }

                val block = scheduleRepository.getBlock(reminder.scheduleBlockId)
                    ?: run {
                        reminderRepository.dismissReminder(reminder.id)
                        return@launch
                    }

                if (block.status.name in listOf("COMPLETED", "CANCELLED", "SKIPPED")) {
                    reminderRepository.dismissReminder(reminder.id)
                    return@launch
                }

                if (!reminderRepository.markFiring(reminder.id, now)) return@launch
                val latest = reminderRepository.getReminder(reminder.id) ?: return@launch
                DayflowAlarmNotifications.show(context, latest, block.title)
            } catch (error: Exception) {
                Log.e(TAG, "Could not deliver Dayflow task reminder $reminderId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.dayflow.action.FIRE_TASK_ALARM"
        const val EXTRA_REMINDER_ID = "com.dayflow.extra.REMINDER_ID"
        private const val TAG = "DayflowTaskAlarm"
        private const val EARLY_TOLERANCE_MILLIS = 1_000L
    }
}
