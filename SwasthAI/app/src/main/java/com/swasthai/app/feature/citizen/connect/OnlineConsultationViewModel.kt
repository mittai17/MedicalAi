package com.swasthai.app.feature.citizen.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.ConsultationRequest
import com.swasthai.app.domain.repository.ConsultationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the online consultation request tracker.
 *
 * Requests are stored offline-first and queued for backend sync; the list
 * reflects the local source of truth until the backend confirms.
 */
@HiltViewModel
class OnlineConsultationViewModel @Inject constructor(
    private val repository: ConsultationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    data class ConsultationUiState(
        val requests: List<ConsultationRequest> = emptyList(),
        val submitting: Boolean = false,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(ConsultationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = userPreferences.userIdFlow.first() ?: userPreferences.stableUserId()
            repository.getRequestsByUser(userId).collect { list ->
                _uiState.update { it.copy(requests = list) }
            }
        }
    }

    fun submitRequest(reason: String, urgency: String) {
        if (reason.isBlank() || _uiState.value.submitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, message = null) }
            val userId = userPreferences.userIdFlow.first() ?: userPreferences.stableUserId()
            val result = repository.submitRequest(
                ConsultationRequest(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    reason = reason.trim(),
                    urgency = urgency,
                    patientName = "Self",
                    createdAt = System.currentTimeMillis()
                )
            )
            _uiState.update {
                it.copy(
                    submitting = false,
                    message = result.fold(
                        onSuccess = { "Consultation request submitted" },
                        onFailure = { e -> "Couldn't save request: ${e.message}" }
                    )
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}