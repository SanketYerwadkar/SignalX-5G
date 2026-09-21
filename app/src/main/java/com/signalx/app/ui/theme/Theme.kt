package com.signalx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Extra semantic colors that Material's ColorScheme doesn't have. */
@Immutable
class SignalXColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,       // cards and tiles
    val surfaceHigh: Color,
    val outline: Color,       // hairline borders, gauge track
    val textPrimary: Color,
    val textMuted: Color,
    val accent: Color,        // cyan
    val blue: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    val heroTop: Color,       // hero card gradient
    val heroBottom: Color
)

val DarkSignalXColors = SignalXColors(
    isDark = true,
    background = Color(0xFF05070A),
    surface = Color(0xFF0D1320),
    surfaceHigh = Color(0xFF141D2E),
    outline = Color(0xFF1D2A42),
    textPrimary = Color(0xFFEAF0FA),
    textMuted = Color(0xFF8A94A6),
    accent = Color(0xFF22D3EE),
    blue = Color(0xFF3B7BFF),
    good = Color(0xFF34D399),
    warn = Color(0xFFFBBF24),
    bad = Color(0xFFF87171),
    heroTop = Color(0xFF111B2E),
    heroBottom = Color(0xFF0A101C)
)

val LightSignalXColors = SignalXColors(
    isDark = false,
    background = Color(0xFFF3F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE9EFF8),
    outline = Color(0xFFD3DCEA),
    textPrimary = Color(0xFF0B1220),
    textMuted = Color(0xFF58627A),
    accent = Color(0xFF0891B2),   // darker cyan so it stays readable on white
    blue = Color(0xFF2F5BFF),
    good = Color(0xFF059669),
    warn = Color(0xFFB45309),
    bad = Color(0xFFDC2626),
    heroTop = Color(0xFFFFFFFF),
    heroBottom = Color(0xFFE8F3FA)
)

val LocalSignalXColors = staticCompositionLocalOf { DarkSignalXColors }

/** Use as `SignalX.colors.accent` anywhere inside SignalXTheme. */
object SignalX {
    val colors: SignalXColors
        @Composable @ReadOnlyComposable get() = LocalSignalXColors.current
}

enum class ThemeMode {
    DARK, LIGHT, SYSTEM;

    companion object {
        val Dark get() = DARK
        val Light get() = LIGHT
        val System get() = SYSTEM
    }
}

@Composable
fun SignalXTheme(
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkSignalXColors else LightSignalXColors

    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = Color(0xFF03141A),
            background = c.background,
            onBackground = c.textPrimary,
            surface = c.background,           // nav bar / app bar blend with the page
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceHigh,
            onSurfaceVariant = c.textMuted,
            outline = c.outline,
            secondaryContainer = Color(0xFF0F2E3F), // selected nav pill: cyan tint, not purple
            onSecondaryContainer = c.accent
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = Color.White,
            background = c.background,
            onBackground = c.textPrimary,
            surface = c.background,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceHigh,
            onSurfaceVariant = c.textMuted,
            outline = c.outline,
            secondaryContainer = Color(0xFFD5F1F8),
            onSecondaryContainer = c.accent
        )
    }

    // Dark status-bar icons on the light theme, light icons on the dark theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(LocalSignalXColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = SxTypography, content = content)
    }
}
