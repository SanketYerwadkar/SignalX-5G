package com.signalx.app.data.repository

import com.signalx.app.domain.model.*
import com.signalx.app.domain.repository.NetworkRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * PHASE 1 ONLY. Realistic mock data so every screen is navigable before any
 * telephony API is touched. Nothing here ships in Phase 2.
 */
class MockNetworkRepository : NetworkRepository {

    private val _state = MutableStateFlow<NetworkUiState>(NetworkUiState.Loading)
    override val state: StateFlow<NetworkUiState> = _state

    /** Flip these to preview the empty / error / permission screens. */
    var scenario: Scenario = Scenario.SINGLE_SIM_5G

    enum class Scenario { SINGLE_SIM_5G, DUAL_SIM_5G, SINGLE_SIM_LTE, NO_SIM, NO_SERVICE, PERMISSION_DENIED, ERROR }

    private var selectedSlot = 0

    init { snapshotNow() }

    override suspend fun refresh() {
        val current = _state.value
        if (current is NetworkUiState.Ready) _state.value = current.copy(refreshing = true)
        delay(900)                       // simulates a real measurement round-trip
        snapshotNow()
    }

    override fun selectSim(slotIndex: Int) {
        selectedSlot = slotIndex
        _state.update {
            if (it is NetworkUiState.Ready) it.copy(snapshot = it.snapshot.copy(selectedSlot = slotIndex)) else it
        }
    }

    private fun snapshotNow() {
        _state.value = when (scenario) {
            Scenario.NO_SIM -> NetworkUiState.NoSim
            Scenario.NO_SERVICE -> NetworkUiState.NoService
            Scenario.PERMISSION_DENIED -> NetworkUiState.PermissionRequired
            Scenario.ERROR -> NetworkUiState.Error("Unable to read network information.")
            Scenario.SINGLE_SIM_5G -> ready(listOf(sim1(jitter())))
            Scenario.SINGLE_SIM_LTE -> ready(listOf(sim2(jitter())))
            Scenario.DUAL_SIM_5G -> ready(listOf(sim1(jitter()), sim2(jitter())))
        }
    }

    private fun ready(sims: List<SimInfo>) = NetworkUiState.Ready(
        NetworkSnapshot(sims, sims.firstOrNull { it.slotIndex == selectedSlot }?.slotIndex ?: sims.first().slotIndex,
            System.currentTimeMillis())
    )

    private fun jitter() = Random.nextInt(-4, 5)

    private fun sim1(j: Int) = SimInfo(
        slotIndex = 0, displayName = "SIM 1", operator = "Jio",
        connectionType = ConnectionType.NR_5G, radioTech = RadioTech.NR,
        signalDbm = -82 + j, dataState = DataState.CONNECTED,
        voiceNetwork = "VoNR", roaming = false, isDefaultData = true,
        nr = NrDetails("Connected", -82 + j, -10, 18, "n78 (3500 MHz)", "0x1A2B3C4"),
        lte = LteDetails(-91, -12, 15, "B3 (1800 MHz)", "0x0C4E19", "4301")
    )

    private fun sim2(j: Int) = SimInfo(
        slotIndex = 1, displayName = "SIM 2", operator = "Airtel",
        connectionType = ConnectionType.LTE_4G, radioTech = RadioTech.LTE,
        signalDbm = -97 + j, dataState = DataState.DISCONNECTED,
        voiceNetwork = "VoLTE", roaming = false, isDefaultData = false,
        nr = NrDetails(),                      // renders as "Not available on this device"
        lte = LteDetails(-97 + j, -14, 9, "B40 (2300 MHz)", "0x0A17F2", "2907")
    )
}
