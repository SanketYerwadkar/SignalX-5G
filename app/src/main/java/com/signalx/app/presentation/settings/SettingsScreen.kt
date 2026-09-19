package com.signalx.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.signalx.app.domain.model.AppSettings
import com.signalx.app.domain.model.RefreshInterval
import com.signalx.app.ui.components.SectionLabel
import com.signalx.app.ui.components.SxCard
import com.signalx.app.ui.theme.Sx
import com.signalx.app.ui.theme.SxColor
import com.signalx.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    versionName: String,
    onTheme: (ThemeMode) -> Unit,
    onInterval: (RefreshInterval) -> Unit,
    onConfirmToggle: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(Sx.s4).padding(bottom = Sx.s8),
            verticalArrangement = Arrangement.spacedBy(Sx.s5)
        ) {
            Column {
                SectionLabel("Appearance")
                SxCard {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(Sx.s2))
                    ThemeMode.entries.forEach { mode ->
                        ChoiceRow(
                            label = when (mode) {
                                ThemeMode.DARK -> "Dark"; ThemeMode.LIGHT -> "Light"; ThemeMode.SYSTEM -> "System default"
                            },
                            selected = settings.themeMode == mode
                        ) { onTheme(mode) }
                    }
                }
            }
            Column {
                SectionLabel("Network")
                SxCard {
                    Text("Auto refresh", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Refreshes only while the app is in the foreground.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Sx.s2))
                    RefreshInterval.entries.forEach { interval ->
                        ChoiceRow(interval.label, settings.refreshInterval == interval) { onInterval(interval) }
                    }
                }
            }
            Column {
                SectionLabel("Behavior")
                SxCard {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Sx.touchMin),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Confirm before opening settings", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Show the 5G explanation sheet first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.confirmBeforeOpeningSettings, onCheckedChange = onConfirmToggle)
                    }
                }
            }
            Column {
                SectionLabel("About")
                SxCard {
                    LinkRow("SignalX 5G", versionName, onAbout)
                    LinkRow("Privacy", null, onPrivacy)
                    LinkRow("Open source licenses", null, onLicenses)
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Sx.touchMin).clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(Sx.s2))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkRow(label: String, trailing: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Sx.touchMin).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        trailing?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SxColor.TextSecondary) }
    }
}
