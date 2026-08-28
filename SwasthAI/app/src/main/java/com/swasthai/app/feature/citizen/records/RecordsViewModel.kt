package com.swasthai.app.feature.citizen.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Report
import com.swasthai.app.domain.model.Screening
import com.swasthai.app.domain.model.ScreeningType
import com.swasthai.app.domain.repository.ReportRepository
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RecordsUiState(
    val screenings: List<ScreeningRecord> = emptyList(),
    val reports: List<Report> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTabIndex: Int = 0,
    val error: String? = null
)

data class ScreeningRecord(
    val id: String,
    val title: String,
    val date: String,
    val riskLevel: RiskLevel,
    val screeningType: ScreeningType,
    val disease: String?
)

/**
 * ViewModel for Health Records screen.
 * Fetches past screenings and generated reports.
 */
@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    private val reportRepository: ReportRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            val userId = userPreferences.userIdFlow.first() ?: ""
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                combine(
                    screeningRepository.getScreeningsByUser(userId),
                    reportRepository.getAllReports()
                ) { screenings, reports ->
                    RecordsUiState(
                        screenings = screenings.map { it.toRecord() },
                        reports = reports,
                        isLoading = false
                    )
                }.collect { _uiState.value = it }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Could not load your health records."
                )
            }
        }
    }

    fun retry() {
        loadRecords()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }

    private fun Screening.toRecord() = ScreeningRecord(
        id = id,
        title = when (screeningType) {
            ScreeningType.SYMPTOM_CHECK -> "Symptom Check"
            ScreeningType.VOICE_ASSISTANT -> "Voice Screening"
            ScreeningType.IMAGE_CHECK -> "Image Screening"
            ScreeningType.COMBINED -> "Combined Screening"
        },
        date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAt)),
        riskLevel = diagnosisResult?.riskLevel ?: RiskLevel.LOW,
        screeningType = screeningType,
        disease = diagnosisResult?.predictedDisease
    )
}
