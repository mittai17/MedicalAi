package com.swasthai.app.feature.citizen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.core.utils.NetworkMonitor
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.DiagnosisResult
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Screening
import com.swasthai.app.domain.model.ScreeningStatus
import com.swasthai.app.domain.model.ScreeningType
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Citizen Dashboard.
 */
data class CitizenDashboardUiState(
    val userName: String = "",
    val isOnline: Boolean = true,
    val recentScreenings: List<ScreeningDisplayItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Display model for screening items shown on the dashboard.
 */
data class ScreeningDisplayItem(
    val id: String,
    val title: String,
    val date: String,
    val riskLevel: RiskLevel,
    val screeningType: ScreeningType
)

/**
 * ViewModel for Citizen Dashboard (Screen 04).
 *
 * Fetches user greeting, online/offline status,
 * and recent screening history for display.
 */
@HiltViewModel
class CitizenDashboardViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    private val userPreferences: UserPreferences,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenDashboardUiState())
    val uiState: StateFlow<CitizenDashboardUiState> = _uiState

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val userId = userPreferences.userIdFlow.first() ?: ""
            val userName = userPreferences.userNameFlow.first() ?: "User"

            // Combine network status and recent screenings
            combine(
                networkMonitor.isOnline,
                screeningRepository.getRecentScreenings(userId, limit = 5)
            ) { isOnline, screenings ->
                CitizenDashboardUiState(
                    userName = userName,
                    isOnline = isOnline,
                    recentScreenings = screenings.map { it.toDisplayItem() },
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun Screening.toDisplayItem(): ScreeningDisplayItem {
        val title = when (screeningType) {
            ScreeningType.SYMPTOM_CHECK -> "Symptom Check"
            ScreeningType.VOICE_ASSISTANT -> "Voice Screening"
            ScreeningType.IMAGE_CHECK -> "Image Screening"
            ScreeningType.COMBINED -> "Combined Screening"
        }
        val riskLevel = diagnosisResult?.riskLevel ?: RiskLevel.LOW
        val dateStr = formatDate(createdAt)
        return ScreeningDisplayItem(
            id = id,
            title = title,
            date = dateStr,
            riskLevel = riskLevel,
            screeningType = screeningType
        )
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
