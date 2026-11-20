package com.dayflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import com.dayflow.feature.today.presentation.DayflowApp
import com.dayflow.feature.today.presentation.TodayViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val todayViewModel = ViewModelProvider(this)[TodayViewModel::class.java]

        setContent {
            DayflowApp(viewModel = todayViewModel)
        }
    }
}
