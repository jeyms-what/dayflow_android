package com.dayflow.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object DayflowNotificationChannels {
    const val TASK_ALARMS = "dayflow_task_alarms"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            TASK_ALARMS,
            "Task alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Audible and vibrating reminders for scheduled Dayflow tasks."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            setSound(alarmSound, audioAttributes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }
}
