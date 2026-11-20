package com.dayflow.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dayflow.MainActivity
import com.dayflow.domain.model.TaskReminder

object DayflowAlarmNotifications {

    fun show(
        context: Context,
        reminder: TaskReminder,
        taskTitle: String
    ) {
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(
            context,
            DayflowNotificationChannels.TASK_ALARMS
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for $taskTitle")
            .setContentText("Your scheduled task is starting now.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Complete",
                actionIntent(context, reminder, TaskAlarmActionReceiver.ACTION_COMPLETE, 1)
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze ${reminder.snoozeMinutes} min",
                actionIntent(context, reminder, TaskAlarmActionReceiver.ACTION_SNOOZE, 2)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                actionIntent(context, reminder, TaskAlarmActionReceiver.ACTION_DISMISS, 3)
            )
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(reminder.requestCode, notification)
    }

    fun cancel(context: Context, requestCode: Int) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(requestCode)
    }

    private fun actionIntent(
        context: Context,
        reminder: TaskReminder,
        action: String,
        salt: Int
    ): PendingIntent {
        val intent = Intent(context, TaskAlarmActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("dayflow://reminder/${reminder.id}/$salt")
            putExtra(TaskAlarmActionReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(TaskAlarmActionReceiver.EXTRA_REQUEST_CODE, reminder.requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            "${reminder.id}:$salt".hashCode() and Int.MAX_VALUE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
