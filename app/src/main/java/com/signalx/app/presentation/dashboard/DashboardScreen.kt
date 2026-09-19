package com.signalx.app.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.signalx.app.ads.AdManager
import com.signalx.app.domain.model.*
import com.signalx.app.service.AutomationEvents
import com.signalx.app.service.SignalXAccessibilityService
import com.signalx.app.ui.components.*
import com.signalx.app.ui.theme.Sx
import com.signalx.app.ui.theme.SxColor
import com.signalx.app.utils.SettingsIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: NetworkUiState,
    confirmBeforeOpening: Boolean,
    onRefresh: () -> Unit,
    onSelectSim: (Int) -> Unit,
    onOpenNetworkDetails: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onGrantPermission: () -> Unit
) {
    val context = LocalContext.current
    var showFiveGSheet by rememberSaveable { mutableStateOf(false) }
    var settingsUnavailable by rememberSaveable { mutableStateOf(false) }
    var showAccessibilityPrompt by rememberSaveable { mutableStateOf(false) }
    var successDialogMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var pendingSuccessMessage by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                pendingSuccessMessage?.let { msg ->
                    pendingSuccessMessage = null
                    val activity = context as? Activity
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        AdManager.showInterstitial(activity) {
                            successDialogMessage = msg
                        }
                    }, 500)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        AutomationEvents.events.collect { message: String ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                val activity = context as? Activity
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    AdManager.showInterstitial(activity) {
                        successDialogMessage = message
                    }
                }, 400)
            } else {
                pendingSuccessMessage = message
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SignalXLogo(fontSize = 20) },
                actions = {
                    IconButton(onClick = onOpenAbout) { Icon(Icons.Default.Info, "About") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when (state) {
            NetworkUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = SxColor.Cyan)
            }
            NetworkUiState.PermissionRequired -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                StateMessage(
                    "Permission needed",
                    "Network information permission is required to display detailed cellular information.",
                    "Grant Permission", onGrantPermission, "Not Now", onRefresh
                )
            }
            NetworkUiState.NoSim -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                StateMessage("No SIM detected", "Insert a SIM card to view cellular network information.", "Refresh", onRefresh)
            }
            NetworkUiState.NoService -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                StateMessage("No Cellular Connection", "Your device is not registered on a mobile network. Wi-Fi and device information remain available.", "Refresh", onRefresh)
            }
            is NetworkUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                StateMessage("Something went wrong", state.message, "Try Again", onRefresh)
            }
            is NetworkUiState.Ready -> DashboardContent(
                state = state,
                padding = padding,
                onRefresh = onRefresh,
                onSelectSim = onSelectSim,
                onOpenNetworkDetails = onOpenNetworkDetails,
                onOpenAbout = onOpenAbout,
                onFiveGTap = {
                    if (SignalXAccessibilityService.isServiceEnabled(context)) {
                        SignalXAccessibilityService.startAutoConfigure()
                        if (!SettingsIntents.openRadioInfo(context)) {
                            settingsUnavailable = true
                        }
                    } else {
                        showAccessibilityPrompt = true
                    }
                },
                onQuickSetting = { target ->
                    if (!SettingsIntents.open(context, target)) settingsUnavailable = true
                }
            )
        }
    }

    if (showAccessibilityPrompt) {
        AlertDialog(
            onDismissRequest = { showAccessibilityPrompt = false },
            title = { Text("Enable Auto-5G (No Root)") },
            text = {
                Text(
                    "To automatically select 'NR only' and refresh SMSC without touching anything, enable SignalX in Accessibility Settings:\n\n" +
                        "1. Tap 'Enable Auto-5G' below\n" +
                        "2. Tap 'SignalX⁵ᴳ' in the list\n" +
                        "3. Turn the switch ON\n\n" +
                        "Or tap 'Open Manually' to select NR only yourself in Phone Info."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityPrompt = false
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        SettingsIntents.openRadioInfo(context)
                    }
                }) {
                    Text("Enable Auto-5G")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAccessibilityPrompt = false
                    if (!SettingsIntents.openRadioInfo(context)) settingsUnavailable = true
                }) {
                    Text("Open Manually")
                }
            }
        )
    }

    if (showFiveGSheet) {
        FiveGSettingsSheet(
            onDismiss = { showFiveGSheet = false },
            onOpen = {
                showFiveGSheet = false
                if (SignalXAccessibilityService.isServiceEnabled(context)) {
                    SignalXAccessibilityService.startAutoConfigure()
                }
                if (!SettingsIntents.openRadioInfo(context)) settingsUnavailable = true
            }
        )
    }

    if (settingsUnavailable) {
        AlertDialog(
            onDismissRequest = { settingsUnavailable = false },
            title = { Text("Settings unavailable") },
            text = { Text("Could not open Phone Info automatically. You can also dial *#*#4636#*#* in your phone dialer to access Phone Info directly.") },
            confirmButton = { TextButton({ settingsUnavailable = false }) { Text("OK") } }
        )
    }

    if (successDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { successDialogMessage = null },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SxColor.Success,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("5G Configured Successfully") },
            text = {
                Text(
                    "Network has been set to 5G (NR only) on Phone 0 and SMSC has been refreshed successfully."
                )
            },
            confirmButton = {
                Button(onClick = { successDialogMessage = null }) {
                    Text("Awesome!")
                }
            }
        )
    }
}

@Composable
private fun DashboardContent(
    state: NetworkUiState.Ready,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onSelectSim: (Int) -> Unit,
    onOpenNetworkDetails: () -> Unit,
    onOpenAbout: () -> Unit,
    onFiveGTap: () -> Unit,
    onQuickSetting: (SettingsIntents.Target) -> Unit
) {
    val sim = state.snapshot.selected ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(Sx.s4, Sx.s2, Sx.s4, Sx.s8),
        verticalArrangement = Arrangement.spacedBy(Sx.s5)
    ) {
        if (state.snapshot.sims.size > 1) {
            item { SimSelector(state.snapshot.sims, sim.slotIndex, onSelectSim) }
        }
        item {
            SectionLabel("Current Network")
            CurrentNetworkCard(sim, state.refreshing, onRefresh)
        }
        item { SetTo5GCard(onFiveGTap) }
        item {
            SectionLabel("Network Status")
            SxCard(onClick = onOpenNetworkDetails) {
                InfoRow("Operator", sim.operator)
                InfoRow("Radio", sim.radioTech.label)
                InfoRow("Signal", sim.signalDbm?.let { "$it dBm" }, mono = true)
                InfoRow("SIM", sim.displayName)
                InfoRow("Data", sim.dataState.label)
                InfoRow("Voice", sim.voiceNetwork)
                InfoRow("Roaming", sim.roaming?.let { if (it) "On" else "Off" })
            }
        }
        item {
            SectionLabel("Quick Actions")
            QuickActions(
                onFiveG = onFiveGTap,
                onSim = { onQuickSetting(SettingsIntents.Target.SIM) },
                onInfo = onOpenNetworkDetails,
                onRefresh = onRefresh,
                onAbout = onOpenAbout
            )
        }
    }
}

@Composable
private fun SimSelector(sims: List<SimInfo>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Sx.s2)) {
        sims.forEach { s ->
            val active = s.slotIndex == selected
            FilterChip(
                selected = active,
                onClick = { onSelect(s.slotIndex) },
                label = { Text("${s.displayName} - ${s.operator ?: "No operator"}") },
                shape = Sx.rPill,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SxColor.Cyan.copy(alpha = 0.15f),
                    selectedLabelColor = SxColor.Cyan
                )
            )
        }
    }
}

@Composable
private fun CurrentNetworkCard(sim: SimInfo, refreshing: Boolean, onRefresh: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spinDeg"
    ).value
    SxCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    sim.connectionType.label,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Sx.s2))
                StatusChip(
                    if (sim.connected) "Connected" else sim.dataState.label,
                    if (sim.connected) SxColor.Success else SxColor.TextTertiary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                SignalBars(sim.quality, barHeight = 34)
                Spacer(Modifier.height(Sx.s2))
                Text(
                    sim.signalDbm?.let { "$it dBm" } ?: "-- dBm",
                    style = com.signalx.app.ui.theme.SxMono,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(sim.quality.label, style = MaterialTheme.typography.bodyMedium, color = sim.quality.color())
            }
        }
        Spacer(Modifier.height(Sx.s3))
        Divider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier.fillMaxWidth().heightIn(min = Sx.touchMin).clickable(onClick = onRefresh),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Refresh, null,
                Modifier.size(18.dp).rotate(if (refreshing) spin else 0f),
                tint = SxColor.Cyan
            )
            Spacer(Modifier.width(Sx.s2))
            Text(
                if (refreshing) "Refreshing..." else "Refresh",
                style = MaterialTheme.typography.labelLarge, color = SxColor.Cyan
            )
        }
    }
}

/** The primary action. Honest copy: it opens configuration, it does not force a mode. */
@Composable
private fun SetTo5GCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Sx.rLg)
            .background(SxColor.AccentGradient)
            .clickable(onClick = onClick)
            .padding(Sx.s5)
            .semantics { contentDescription = "Set network to 5G. Opens network configuration." },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(SxColor.Background.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) { Text("5G", fontWeight = FontWeight.Bold, color = SxColor.TextPrimary) }
        Spacer(Modifier.width(Sx.s4))
        Column(Modifier.weight(1f)) {
            Text("Set Network to 5G", style = MaterialTheme.typography.titleMedium, color = SxColor.TextPrimary)
            Text("Open network configuration", style = MaterialTheme.typography.bodyMedium, color = SxColor.TextPrimary.copy(alpha = 0.8f))
        }
        Icon(Icons.Default.KeyboardArrowRight, null, tint = SxColor.TextPrimary)
    }
}

@Composable
private fun QuickActions(
    onFiveG: () -> Unit, onSim: () -> Unit, onInfo: () -> Unit,
    onRefresh: () -> Unit, onAbout: () -> Unit
) {
    val items = listOf(
        Triple("5G Settings", Icons.Default.Star, onFiveG),
        Triple("SIM Settings", Icons.Default.Call, onSim),
        Triple("Network Info", Icons.Default.List, onInfo),
        Triple("Refresh", Icons.Default.Refresh, onRefresh),
        Triple("About", Icons.Default.Info, onAbout)
    )
    Column(verticalArrangement = Arrangement.spacedBy(Sx.s2)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Sx.s2)) {
                row.forEach { (label, icon, action) -> QuickAction(label, icon, action, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(Sx.rMd)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .heightIn(min = 84.dp)
            .padding(Sx.s4),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = SxColor.Cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(Sx.s2))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
