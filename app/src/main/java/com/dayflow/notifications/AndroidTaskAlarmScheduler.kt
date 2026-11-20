package com.dayflow.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.dayflow.domain.model.TaskReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTaskAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : TaskAlarmScheduler {

    override fun schedule(reminder: TaskReminder): AlarmPrecision {
        require(reminder.id.isNotBlank())
        require(reminder.requestCode > 0)
        require(reminder.triggerAtEpochMillis > 0L)

        val alarmManager = context.getSystemService(AlarmManager::class.java)
            ?: error("AlarmManager is unavailable.")
        val pendingIntent = alarmPendingIntent(reminder.id, reminder.requestCode)

        val exactAccessAvailable =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()

        if (exactAccessAvailable) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtEpochMillis,
                    pendingIntent
                )
                return AlarmPrecision.EXACT
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and scheduling.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.triggerAtEpochMillis,
            pendingIntent
        )
        return AlarmPrecision.INEXACT
    }

    override fun cancel(reminder: TaskReminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmPendingIntent(reminder.id, reminder.requestCode)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun cancelNotification(requestCode: Int) {
        context.getSystemService(android.app.NotificationManager::class.java)
            ?.cancel(requestCode)
    }

    private fun alarmPendingIntent(
        reminderId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_FIRE
            data = Uri.parse("dayflow://reminder/$reminderId/fire")
            putExtra(TaskAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
