package com.dayflow

import android.app.Application
import com.dayflow.notifications.DayflowNotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DayflowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DayflowNotificationChannels.create(this)
    }
}
