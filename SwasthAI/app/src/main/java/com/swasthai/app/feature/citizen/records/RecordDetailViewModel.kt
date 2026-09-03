package com.swasthai.app.feature.citizen.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.domain.model.ScreeningDetail
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for a single screening's detail view.
 */
data class RecordDetailUiState(
    val isLoading: Boolean = true,
    val detail: ScreeningDetail? = null,
    val error: String? = null
)

/**
 * Loads and exposes the fully-composed detail for one screening:
 * symptoms, vitals, diagnosis (with recommendations) and images.
 */
@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordDetailUiState())
    val uiState: StateFlow<RecordDetailUiState> = _uiState

    fun load(screeningId: String) {
        viewModelScope.launch {
            _uiState.value = RecordDetailUiState(isLoading = true)
            try {
                val detail = screeningRepository.getScreeningDetail(screeningId)
                _uiState.value = if (detail != null) {
                    RecordDetailUiState(isLoading = false, detail = detail)
                } else {
                    RecordDetailUiState(
                        isLoading = false,
                        error = "This screening could not be found."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RecordDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Could not load this screening."
                )
            }
        }
    }
}