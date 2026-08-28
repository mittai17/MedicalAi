package com.swasthai.app.feature.healthworker.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Referral
import com.swasthai.app.domain.repository.ReportRepository
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = true,
    val screeningsThisMonth: Int = 0,
    val referralsThisMonth: Int = 0,
    val highRiskThisMonth: Int = 0,
    val pendingReferralsList: List<Referral> = emptyList()
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    private val reportRepository: ReportRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val userId = userPreferences.userIdFlow.first()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis

            val screenings = screeningRepository.getScreeningCountInRange(userId, monthStart, now)
            val highRisk = screeningRepository.getHighRiskCountInRange(userId, monthStart, now)
            val referralList = reportRepository.getPendingReferrals().first()

            _uiState.value = ReportsUiState(
                isLoading = false,
                screeningsThisMonth = screenings,
                referralsThisMonth = referralList.size,
                highRiskThisMonth = highRisk,
                pendingReferralsList = referralList
            )
        }
    }
}
