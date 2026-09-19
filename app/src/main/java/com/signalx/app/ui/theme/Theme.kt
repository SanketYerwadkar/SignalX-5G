package com.signalx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { DARK, LIGHT, SYSTEM }

private val DarkScheme = darkColorScheme(
    primary = SxColor.Cyan, onPrimary = SxColor.Background,
    secondary = SxColor.Blue, onSecondary = SxColor.TextPrimary,
    background = SxColor.Background, onBackground = SxColor.TextPrimary,
    surface = SxColor.Surface, onSurface = SxColor.TextPrimary,
    surfaceVariant = SxColor.SurfaceElevated, onSurfaceVariant = SxColor.TextSecondary,
    outline = SxColor.Outline, error = SxColor.Danger
)

private val LightScheme = lightColorScheme(
    primary = SxColor.BlueInk, onPrimary = SxColor.LSurface,
    secondary = SxColor.Blue,
    background = SxColor.LBackground, onBackground = SxColor.LTextPrimary,
    surface = SxColor.LSurface, onSurface = SxColor.LTextPrimary,
    surfaceVariant = SxColor.LSurfaceVariant, onSurfaceVariant = SxColor.LTextSecondary,
    outline = SxColor.LOutline, error = SxColor.Danger
)

@Composable
fun SignalXTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = scheme, typography = SxTypography, content = content)
}
