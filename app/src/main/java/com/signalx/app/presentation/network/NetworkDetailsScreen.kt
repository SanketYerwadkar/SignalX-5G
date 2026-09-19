package com.signalx.app.presentation.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.signalx.app.domain.model.NetworkUiState
import com.signalx.app.domain.model.SimInfo
import com.signalx.app.ui.components.*
import com.signalx.app.ui.theme.Sx
import com.signalx.app.ui.theme.SxColor
import com.signalx.app.utils.SettingsIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailsScreen(state: NetworkUiState, onSelectSim: (Int) -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current
    var unavailable by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Network Details") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (state !is NetworkUiState.Ready) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                StateMessage(
                    "Network information unavailable",
                    "Your device does not expose this information right now.",
                    "Refresh", onRefresh
                )
            }
            return@Scaffold
        }
        val sim = state.snapshot.selected ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Sx.s4, Sx.s2, Sx.s4, Sx.s8),
            verticalArrangement = Arrangement.spacedBy(Sx.s3)
        ) {
            item { SignalSummary(sim) }
            item {
                ExpandableCard("Connection", sim.connectionType.label, initiallyExpanded = true) {
                    InfoRow("Connection type", sim.connectionType.label)
                    InfoRow("Radio technology", sim.radioTech.label)
                    InfoRow("Data state", sim.dataState.label)
                    InfoRow("Voice network", sim.voiceNetwork)
                    InfoRow("Roaming", sim.roaming?.let { if (it) "On" else "Off" })
                }
            }
            item {
                ExpandableCard("SIM & Operator", sim.displayName) {
                    InfoRow("SIM slot", sim.displayName)
                    InfoRow("Operator", sim.operator)
                    InfoRow("Default data SIM", if (sim.isDefaultData) "Yes" else "No")
                }
            }
            item {
                ExpandableCard("5G / NR", if (sim.nr.available) sim.nr.status ?: "Available" else "Not available") {
                    if (!sim.nr.available) {
                        Text(
                            "5G information is not currently available on this device or SIM.",
                            style = MaterialTheme.typography.bodyMedium, color = SxColor.TextTertiary
                        )
                    } else {
                        InfoRow("Status", sim.nr.status)
                        InfoRow("SS-RSRP", sim.nr.ssRsrpDbm?.let { "$it dBm" }, mono = true)
                        InfoRow("SS-RSRQ", sim.nr.ssRsrqDb?.let { "$it dB" }, mono = true)
                        InfoRow("SS-SINR", sim.nr.ssSinrDb?.let { "$it dB" }, mono = true)
                        InfoRow("Band", sim.nr.band)
                        InfoRow("Cell identity", sim.nr.cellId, mono = true)
                    }
                }
            }
            item {
                ExpandableCard("LTE", if (sim.lte.available) "Available" else "Not available") {
                    InfoRow("RSRP", sim.lte.rsrpDbm?.let { "$it dBm" }, mono = true)
                    InfoRow("RSRQ", sim.lte.rsrqDb?.let { "$it dB" }, mono = true)
                    InfoRow("SINR", sim.lte.sinrDb?.let { "$it dB" }, mono = true)
                    InfoRow("Band", sim.lte.band)
                    InfoRow("Cell identity", sim.lte.cellId, mono = true)
                    InfoRow("Tracking area", sim.lte.tac, mono = true)
                }
            }
            item {
                SectionLabel("Network Settings")
                SxCard {
                    listOf(
                        "Phone Info (Force 5G / NR Only)" to SettingsIntents.Target.RADIO_INFO,
                        "Preferred Network Settings" to SettingsIntents.Target.PREFERRED_NETWORK,
                        "Mobile Network Settings" to SettingsIntents.Target.MOBILE_NETWORK,
                        "SIM Settings" to SettingsIntents.Target.SIM,
                        "Data Usage" to SettingsIntents.Target.DATA_USAGE,
                        "Roaming Settings" to SettingsIntents.Target.ROAMING,
                        "Access Point Names" to SettingsIntents.Target.APN,
                        "Network Operators" to SettingsIntents.Target.OPERATORS
                    ).forEach { (label, target) ->
                        SettingsLink(label) { if (!SettingsIntents.open(context, target)) unavailable = true }
                    }
                }
            }
        }
    }

    if (unavailable) {
        AlertDialog(
            onDismissRequest = { unavailable = false },
            title = { Text("Settings unavailable") },
            text = { Text("Your device does not provide a direct shortcut to this setting.") },
            confirmButton = { TextButton({ unavailable = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun SignalSummary(sim: SimInfo) {
    SxCard {
        SectionLabel("Signal")
        Row(verticalAlignment = Alignment.CenterVertically) {
            SignalBars(sim.quality, barHeight = 40)
            Spacer(Modifier.width(Sx.s4))
            Column {
                Text(
                    sim.signalDbm?.let { "$it dBm" } ?: "Not available",
                    style = com.signalx.app.ui.theme.SxMono
                )
                Text(sim.quality.label, style = MaterialTheme.typography.bodyMedium, color = sim.quality.color())
            }
        }
    }
}

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Sx.touchMin)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = SxColor.TextSecondary)
    }
}
