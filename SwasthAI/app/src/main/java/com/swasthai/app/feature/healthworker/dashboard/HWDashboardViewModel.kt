package com.swasthai.app.feature.healthworker.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.core.utils.NetworkMonitor
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Patient
import com.swasthai.app.domain.model.Referral
import com.swasthai.app.domain.repository.PatientRepository
import com.swasthai.app.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HWDashboardUiState(
    val workerName: String = "",
    val isOnline: Boolean = true,
    val totalPatients: Int = 0,
    val pendingReferrals: Int = 0,
    val screeningsToday: Int = 0,
    val criticalAlerts: Int = 0,
    val recentPatients: List<Patient> = emptyList(),
    val pendingReferralList: List<Referral> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for Health Worker Dashboard (Screen 15).
 *
 * Shows: total patients, pending referrals, today's screenings,
 * critical alerts, and recent patient list.
 */
@HiltViewModel
class HWDashboardViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val reportRepository: ReportRepository,
    private val userPreferences: UserPreferences,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HWDashboardUiState())
    val uiState: StateFlow<HWDashboardUiState> = _uiState

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val workerName = userPreferences.userNameFlow.first() ?: "Health Worker"
            combine(
                networkMonitor.isOnline,
                patientRepository.getAllPatients(),
                reportRepository.getPendingReferrals()
            ) { isOnline, patients, pendingReferrals ->
                HWDashboardUiState(
                    workerName = workerName,
                    isOnline = isOnline,
                    totalPatients = patients.size,
                    pendingReferrals = pendingReferrals.size,
                    screeningsToday = patients.count { it.lastScreeningDate != null &&
                        isToday(it.lastScreeningDate!!) },
                    criticalAlerts = pendingReferrals.count { it.priority == "HIGH" },
                    recentPatients = patients.take(5),
                    pendingReferralList = pendingReferrals.take(3),
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = java.util.Calendar.getInstance()
        val cal = java.util.Calendar.getInstance().also { it.timeInMillis = timestamp }
        return today.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR) &&
            today.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR)
    }
}
