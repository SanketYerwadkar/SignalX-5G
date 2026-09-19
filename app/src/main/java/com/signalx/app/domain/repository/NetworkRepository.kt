package com.signalx.app.domain.repository

import com.signalx.app.domain.model.NetworkUiState
import kotlinx.coroutines.flow.Flow

/**
 * The UI depends only on this contract. Phase 1 binds MockNetworkRepository;
 * Phase 2 swaps in a TelephonyNetworkRepository with no UI changes.
 */
interface NetworkRepository {
    val state: Flow<NetworkUiState>
    suspend fun refresh()
    fun selectSim(slotIndex: Int)
}
