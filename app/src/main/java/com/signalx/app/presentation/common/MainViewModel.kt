package com.signalx.app.presentation.common

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.signalx.app.domain.model.AppSettings
import com.signalx.app.domain.model.NetworkUiState
import com.signalx.app.domain.model.RefreshInterval
import com.signalx.app.domain.repository.NetworkRepository
import com.signalx.app.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application, private val repo: NetworkRepository) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("signalx_prefs", Context.MODE_PRIVATE)

    val network: StateFlow<NetworkUiState> =
        repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkUiState.Loading)

    private val _settings = MutableStateFlow(
        AppSettings(themeMode = savedTheme())
    )
    val settings: StateFlow<AppSettings> = _settings

    private var autoJob: Job? = null

    private fun savedTheme(): ThemeMode =
        when (prefs.getString("theme_mode", ThemeMode.DARK.name)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }

    fun refresh() = viewModelScope.launch { repo.refresh() }

    fun selectSim(slot: Int) = repo.selectSim(slot)

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setConfirmBeforeSettings(enabled: Boolean) { _settings.update { it.copy(confirmBeforeOpeningSettings = enabled) } }

    /**
     * Auto-refresh is scoped to viewModelScope, so it stops with the ViewModel and
     * pauses whenever the UI stops collecting (WhileSubscribed) - no background drain.
     */
    fun setRefreshInterval(interval: RefreshInterval) {
        _settings.update { it.copy(refreshInterval = interval) }
        autoJob?.cancel()
        val period = interval.millis ?: return
        autoJob = viewModelScope.launch {
            while (true) { delay(period); repo.refresh() }
        }
    }

    override fun onCleared() { autoJob?.cancel(); super.onCleared() }
}
