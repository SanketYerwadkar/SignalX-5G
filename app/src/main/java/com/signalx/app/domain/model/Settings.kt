package com.signalx.app.domain.model

import com.signalx.app.ui.theme.ThemeMode

enum class RefreshInterval(val label: String, val millis: Long?) {
    OFF("Off", null),
    S30("30 seconds", 30_000),
    S60("60 seconds", 60_000),
    M5("5 minutes", 300_000)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val refreshInterval: RefreshInterval = RefreshInterval.OFF,
    val confirmBeforeOpeningSettings: Boolean = false
)
