package com.dayflow.core.database

import android.content.Context
import androidx.room.Room

/**
 * Temporary manual provider.
 * Replace with a Hilt @Provides binding when DI is introduced.
 * Never expose this singleton to UI composables.
 */
object DatabaseProvider {
    @Volatile
    private var instance: DayflowDatabase? = null

    fun getDatabase(context: Context): DayflowDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DayflowDatabase::class.java,
                "dayflow_database"
            ).build().also { instance = it }
        }
}
