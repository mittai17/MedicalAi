package com.swasthai.app.feature.citizen.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.Report
import com.swasthai.app.domain.model.ScreeningDetail
import com.swasthai.app.domain.repository.ReportRepository
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * UI state for the Share Report screen.
 */
data class ShareReportUiState(
    val isLoading: Boolean = true,
    val detail: ScreeningDetail? = null,
    val error: String? = null,
    val patientName: String = ""
)

/**
 * Loads the screening that a report is being made from and resolves the
 * patient name used on the report header.
 */
@HiltViewModel
class ShareReportViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    private val reportRepository: ReportRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareReportUiState())
    val uiState: StateFlow<ShareReportUiState> = _uiState

    fun load(screeningId: String) {
        viewModelScope.launch {
            _uiState.value = ShareReportUiState(isLoading = true)
            try {
                val detail = screeningRepository.getScreeningDetail(screeningId)
                val name = userPreferences.userNameFlow.first() ?: "Citizen User"
                _uiState.value = if (detail != null) {
                    ShareReportUiState(
                        isLoading = false,
                        detail = detail,
                        patientName = name
                    )
                } else {
                    ShareReportUiState(
                        isLoading = false,
                        error = "This screening could not be found."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ShareReportUiState(
                    isLoading = false,
                    error = e.message ?: "Could not load this screening."
                )
            }
        }
    }

    /**
     * Records the generated report so it shows up in the Reports tab.
     */
    suspend fun persistReport(detail: ScreeningDetail, patientName: String, pdfPath: String?) {
        reportRepository.saveReport(
            Report(
                id = UUID.randomUUID().toString(),
                screeningId = detail.screening.id,
                patientName = patientName,
                reportContent = ReportGenerator.buildLines(detail, patientName).joinToString("\n"),
                pdfPath = pdfPath,
                generatedAt = System.currentTimeMillis()
            )
        )
    }
}