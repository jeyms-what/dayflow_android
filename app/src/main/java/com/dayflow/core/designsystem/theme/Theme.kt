package com.dayflow.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DayflowLightPrimary,
    onPrimary = DayflowLightSurface,
    background = DayflowLightBackground,
    onBackground = DayflowLightOnSurface,
    surface = DayflowLightSurface,
    onSurface = DayflowLightOnSurface,
    onSurfaceVariant = DayflowLightOnSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DayflowDarkPrimary,
    onPrimary = DayflowDarkBackground,
    background = DayflowDarkBackground,
    onBackground = DayflowDarkOnSurface,
    surface = DayflowDarkSurface,
    onSurface = DayflowDarkOnSurface,
    onSurfaceVariant = DayflowDarkOnSurfaceVariant
)

@Composable
fun DayflowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
