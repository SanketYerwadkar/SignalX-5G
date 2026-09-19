package com.signalx.app.domain.model

/**
 * Domain models are deliberately nullable: Android does not expose every value on
 * every device / SIM / API level. A null means "not available on this device" and
 * the UI must render it as such. Never substitute a placeholder number.
 */

enum class ConnectionType(val label: String) {
    NR_5G("5G"), LTE_4G("4G LTE"), WCDMA_3G("3G"), GSM_2G("2G"), UNKNOWN("Unknown")
}

enum class RadioTech(val label: String) {
    NR("NR"), LTE("LTE"), WCDMA("WCDMA"), GSM("GSM"), UNKNOWN("Unknown")
}

enum class SignalQuality(val label: String, val bars: Int) {
    EXCELLENT("Excellent", 5), GOOD("Good", 4), FAIR("Fair", 3), POOR("Poor", 2), UNKNOWN("Unknown", 0);

    companion object {
        /** Coarse dBm mapping; replaced in Phase 2 by SignalStrength.getLevel(). */
        fun fromDbm(dbm: Int?): SignalQuality = when {
            dbm == null -> UNKNOWN
            dbm >= -70 -> EXCELLENT
            dbm >= -85 -> GOOD
            dbm >= -100 -> FAIR
            else -> POOR
        }
    }
}

enum class DataState(val label: String) {
    CONNECTED("Connected"), CONNECTING("Connecting"), DISCONNECTED("Disconnected"), UNKNOWN("Unknown")
}

data class NrDetails(
    val status: String? = null,      // e.g. Connected / Not Restricted / None
    val ssRsrpDbm: Int? = null,
    val ssRsrqDb: Int? = null,
    val ssSinrDb: Int? = null,
    val band: String? = null,
    val cellId: String? = null
) {
    val available: Boolean get() = listOfNotNull(status, ssRsrpDbm, ssRsrqDb, ssSinrDb, band, cellId).isNotEmpty()
}

data class LteDetails(
    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val sinrDb: Int? = null,
    val band: String? = null,
    val cellId: String? = null,
    val tac: String? = null
) {
    val available: Boolean get() = listOfNotNull(rsrpDbm, rsrqDb, sinrDb, band, cellId, tac).isNotEmpty()
}

data class SimInfo(
    val slotIndex: Int,
    val displayName: String,            // "SIM 1"
    val operator: String?,              // "Jio"
    val connectionType: ConnectionType,
    val radioTech: RadioTech,
    val signalDbm: Int?,
    val dataState: DataState,
    val voiceNetwork: String?,          // "VoLTE" / "VoNR"
    val roaming: Boolean?,
    val isDefaultData: Boolean,
    val nr: NrDetails = NrDetails(),
    val lte: LteDetails = LteDetails()
) {
    val quality: SignalQuality get() = SignalQuality.fromDbm(signalDbm)
    val connected: Boolean get() = dataState == DataState.CONNECTED
}

data class NetworkSnapshot(
    val sims: List<SimInfo>,
    val selectedSlot: Int,
    val updatedAtMillis: Long
) {
    val selected: SimInfo? get() = sims.firstOrNull { it.slotIndex == selectedSlot } ?: sims.firstOrNull()
}

/** Every non-happy path the Dashboard has to render. */
sealed interface NetworkUiState {
    data object Loading : NetworkUiState
    data object PermissionRequired : NetworkUiState
    data object NoSim : NetworkUiState
    data object NoService : NetworkUiState
    data class Error(val message: String) : NetworkUiState
    data class Ready(val snapshot: NetworkSnapshot, val refreshing: Boolean = false) : NetworkUiState
}
