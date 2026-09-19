package com.signalx.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                // Step 30 of the flow: returning from system settings re-reads the network.
                androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                    vm.refresh()
                    onPauseOrDispose { }
                }
                DashboardScreen(
                    state = network,
                    confirmBeforeOpening = settings.confirmBeforeOpeningSettings,
                    onRefresh = vm::refresh,
                    onSelectSim = vm::selectSim,
                    onOpenNetworkDetails = { nav.navigate(Routes.NETWORK) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenAbout = { nav.navigate(Routes.ABOUT) },
                    onGrantPermission = { SettingsIntents.open(context, SettingsIntents.Target.APP_DETAILS) }
                )
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
