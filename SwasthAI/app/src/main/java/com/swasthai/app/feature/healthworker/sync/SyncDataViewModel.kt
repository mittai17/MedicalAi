package com.swasthai.app.feature.healthworker.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncDataUiState(
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    val lastSyncSucceeded: Boolean = false,
    val lastSyncMessage: String? = null
)

@HiltViewModel
class SyncDataViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncDataUiState())
    val uiState: StateFlow<SyncDataUiState> = _uiState.asStateFlow()

    private val pendingCount = syncRepository.getPendingSyncCount()

    val pending: StateFlow<Int> = pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, lastSyncMessage = null)
            val result = syncRepository.syncPendingData()
            _uiState.value = _uiState.value.copy(isSyncing = false)
            result.fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        lastSyncSucceeded = true,
                        lastSyncMessage = "$count records synced"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        lastSyncSucceeded = false,
                        lastSyncMessage = e.message ?: "Sync failed"
                    )
                }
            )
        }
    }
}
