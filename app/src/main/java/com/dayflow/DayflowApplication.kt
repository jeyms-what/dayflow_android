package com.dayflow

import android.app.Application

class DayflowApplication : Application() {
    lateinit var container: DayflowContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DayflowContainer(this)
    }
}
