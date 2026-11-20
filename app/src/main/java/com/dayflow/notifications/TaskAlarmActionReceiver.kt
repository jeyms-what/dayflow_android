package com.dayflow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dayflow.domain.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskAlarmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var alarmScheduler: TaskAlarmScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_COMPLETE -> reminderRepository.completeReminder(reminderId)
                    ACTION_SNOOZE -> {
                        val current = reminderRepository.getReminder(reminderId)
                            ?: return@launch
                        val triggerAt = System.currentTimeMillis() +
                            current.snoozeMinutes * 60_000L
                        reminderRepository.snoozeReminder(reminderId, triggerAt)
                        val updated = reminderRepository.getReminder(reminderId)
                            ?: return@launch
                        alarmScheduler.schedule(updated)
                    }
                    ACTION_DISMISS -> reminderRepository.dismissReminder(reminderId)
                    else -> return@launch
                }

                val updated = reminderRepository.getReminder(reminderId)
                if (updated != null) {
                    alarmScheduler.cancelNotification(updated.requestCode)
                    if (action != ACTION_SNOOZE) {
                        alarmScheduler.cancel(updated)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Could not handle Dayflow alarm action $action", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.dayflow.action.COMPLETE_TASK_ALARM"
        const val ACTION_SNOOZE = "com.dayflow.action.SNOOZE_TASK_ALARM"
        const val ACTION_DISMISS = "com.dayflow.action.DISMISS_TASK_ALARM"
        const val EXTRA_REMINDER_ID = "com.dayflow.extra.REMINDER_ID"
        const val EXTRA_REQUEST_CODE = "com.dayflow.extra.REQUEST_CODE"
        private const val TAG = "DayflowAlarmAction"
    }
}
