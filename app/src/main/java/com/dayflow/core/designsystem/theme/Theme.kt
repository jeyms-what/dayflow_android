package com.dayflow.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DayflowLightPrimary,
    onPrimary = Color.White,
    secondary = DayflowLightSecondary,
    tertiary = DayflowLightTertiary,
    background = DayflowLightBackground,
    onBackground = DayflowLightOnSurface,
    surface = DayflowLightSurface,
    onSurface = DayflowLightOnSurface,
    surfaceVariant = DayflowLightSurfaceVariant,
    onSurfaceVariant = DayflowLightOnSurfaceVariant,
    outline = DayflowLightOutline,
    primaryContainer = DayflowLightPrimaryContainer,
    onPrimaryContainer = DayflowLightOnPrimaryContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DayflowDarkPrimary,
    onPrimary = DayflowDarkBackground,
    secondary = DayflowDarkSecondary,
    tertiary = DayflowDarkTertiary,
    background = DayflowDarkBackground,
    onBackground = DayflowDarkOnSurface,
    surface = DayflowDarkSurface,
    onSurface = DayflowDarkOnSurface,
    surfaceVariant = DayflowDarkSurfaceVariant,
    onSurfaceVariant = DayflowDarkOnSurfaceVariant,
    outline = DayflowDarkOutline,
    primaryContainer = DayflowDarkPrimaryContainer,
    onPrimaryContainer = DayflowDarkOnPrimaryContainer
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
