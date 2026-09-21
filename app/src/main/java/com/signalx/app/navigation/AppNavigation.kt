package com.signalx.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.signalx.app.ui.home.toHomeNetworkUiState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.signalx.app.presentation.about.AboutScreen
import com.signalx.app.presentation.about.TextPageScreen
import com.signalx.app.presentation.common.MainViewModel
import com.signalx.app.presentation.dashboard.DashboardScreen
import com.signalx.app.presentation.network.NetworkDetailsScreen
import com.signalx.app.presentation.settings.SettingsScreen
import com.signalx.app.presentation.splash.SplashScreen
import com.signalx.app.utils.SettingsIntents

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val NETWORK = "network"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Default.Home),
    Tab(Routes.NETWORK, "Network", Icons.Default.List),
    Tab(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

@Composable
fun AppNavigation(vm: MainViewModel, versionName: String) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in tabs.map { it.route }
    val context = LocalContext.current

    val network by vm.network.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.SPLASH, modifier = Modifier.padding(padding)) {
            composable(Routes.SPLASH) {
                SplashScreen {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }
            composable(Routes.HOME) {
                // Returning from system settings re-reads the network.
                androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                    vm.refresh()
                    onPauseOrDispose { }
                }

                var settingsUnavailable by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
                var showAccessibilityPrompt by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
                var successDialogMessage by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
                var pendingSuccessMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            pendingSuccessMessage?.let { msg ->
                                pendingSuccessMessage = null
                                val activity = context as? android.app.Activity
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    com.signalx.app.ads.AdManager.showInterstitial(activity) {
                                        successDialogMessage = msg
                                    }
                                }, 500)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    com.signalx.app.service.AutomationEvents.events.collect { message: String ->
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                            val activity = context as? android.app.Activity
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                com.signalx.app.ads.AdManager.showInterstitial(activity) {
                                    successDialogMessage = message
                                }
                            }, 400)
                        } else {
                            pendingSuccessMessage = message
                        }
                    }
                }

                val domainState = network
                val homeState = if (domainState is com.signalx.app.domain.model.NetworkUiState.Ready) {
                    domainState.snapshot.selected?.toHomeNetworkUiState() ?: com.signalx.app.ui.home.NetworkUiState()
                } else {
                    com.signalx.app.ui.home.NetworkUiState()
                }

                com.signalx.app.ui.home.HomeScreen(
                    state = homeState,
                    onRefresh = vm::refresh,
                    onSetTo5G = {
                        if (com.signalx.app.service.SignalXAccessibilityService.isServiceEnabled(context)) {
                            com.signalx.app.service.SignalXAccessibilityService.startAutoConfigure()
                            if (!SettingsIntents.openRadioInfo(context)) {
                                settingsUnavailable = true
                            }
                        } else {
                            showAccessibilityPrompt = true
                        }
                    },
                    onOpen5GSettings = {
                        if (!SettingsIntents.openRadioInfo(context)) settingsUnavailable = true
                    },
                    onOpenSimSettings = {
                        if (!SettingsIntents.open(context, SettingsIntents.Target.SIM)) {
                            SettingsIntents.open(context, SettingsIntents.Target.MOBILE_NETWORK)
                        }
                    },
                    onOpenNetworkInfo = { nav.navigate(Routes.NETWORK) },
                    onAbout = { nav.navigate(Routes.ABOUT) }
                )

                if (showAccessibilityPrompt) {
                    AlertDialog(
                        onDismissRequest = { showAccessibilityPrompt = false },
                        title = { Text("Enable Auto-5G (No Root)") },
                        text = {
                            androidx.compose.foundation.layout.Column {
                                Text(
                                    "To automatically select 'NR only' and refresh SMSC without touching anything, enable SignalX in Accessibility Settings.\n",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "🔒 If Android says 'Restricted setting':\n" +
                                        "1. Tap 'Unlock Restricted Setting' below\n" +
                                        "2. Tap the 3 dots (⋮) in the top-right corner\n" +
                                        "3. Tap 'Allow restricted settings'\n" +
                                        "4. Enter your PIN/Fingerprint, then turn ON Accessibility!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.signalx.app.ui.theme.SignalX.colors.accent
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                showAccessibilityPrompt = false
                                try {
                                    context.startActivity(
                                        android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (e: Exception) {
                                    SettingsIntents.openRadioInfo(context)
                                }
                            }) {
                                Text("1. Open Accessibility")
                            }
                        },
                        dismissButton = {
                            androidx.compose.foundation.layout.Row {
                                TextButton(onClick = {
                                    SettingsIntents.open(context, SettingsIntents.Target.APP_DETAILS)
                                }) {
                                    Text("2. Unlock (3 dots ⋮)")
                                }
                                TextButton(onClick = {
                                    showAccessibilityPrompt = false
                                    if (!SettingsIntents.openRadioInfo(context)) settingsUnavailable = true
                                }) {
                                    Text("Open Manually")
                                }
                            }
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
                                tint = com.signalx.app.ui.theme.SignalX.colors.good,
                                modifier = Modifier.padding(8.dp)
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
            composable(Routes.NETWORK) {
                NetworkDetailsScreen(network, vm::selectSim, vm::refresh)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings, versionName = versionName,
                    onTheme = vm::setTheme, onInterval = vm::setRefreshInterval,
                    onConfirmToggle = vm::setConfirmBeforeSettings,
                    onAbout = { nav.navigate(Routes.ABOUT) },
                    onPrivacy = { nav.navigate(Routes.PRIVACY) },
                    onLicenses = { nav.navigate(Routes.LICENSES) }
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    versionName, { nav.popBackStack() },
                    { nav.navigate(Routes.PRIVACY) }, { nav.navigate(Routes.LICENSES) },
                    { /* Phase 2: mailto intent */ }
                )
            }
            composable(Routes.PRIVACY) {
                TextPageScreen(
                    "Privacy",
                    "SignalX reads cellular network information on this device to display it. " +
                        "Nothing is uploaded, transmitted or stored off-device. (Placeholder copy.)",
                    { nav.popBackStack() }
                )
            }
            composable(Routes.LICENSES) {
                TextPageScreen(
                    "Open Source Licenses",
                    "Licenses for AndroidX, Jetpack Compose and Material 3 will be listed here. (Placeholder.)",
                    { nav.popBackStack() }
                )
            }
        }
    }
}
