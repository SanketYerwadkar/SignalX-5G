package com.signalx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signalx.app.data.repository.MockNetworkRepository
import com.signalx.app.navigation.AppNavigation
import com.signalx.app.presentation.common.MainViewModel
import com.signalx.app.ui.theme.SignalXTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // PHASE 1: mock repository. PHASE 2: swap for TelephonyNetworkRepository(applicationContext).
            val vm: MainViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(MockNetworkRepository()) as T
            })
            val settings by vm.settings.collectAsStateWithLifecycle()
            SignalXTheme(mode = settings.themeMode) {
                AppNavigation(vm, versionName = BuildConfig.VERSION_NAME)
            }
        }
    }
}
