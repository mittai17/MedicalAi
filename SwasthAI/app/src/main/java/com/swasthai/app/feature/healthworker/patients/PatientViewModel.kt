package com.swasthai.app.feature.healthworker.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.domain.model.Patient
import com.swasthai.app.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PatientListUiState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

data class PatientDetailUiState(
    val patient: Patient? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class AddPatientUiState(
    val name: String = "",
    val age: String = "",
    val gender: String = "Male",
    val village: String = "",
    val phone: String = "",
    val aadhar: String = "",
    val isSaving: Boolean = false,
    val savedPatientId: String? = null,
    val error: String? = null
)

/**
 * ViewModel for Patient List, Patient Detail, and Add Patient screens.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientViewModel @Inject constructor(
    private val patientRepository: PatientRepository
) : ViewModel() {

    // ── Patient List ──
    private val _listUiState = MutableStateFlow(PatientListUiState())
    val listUiState: StateFlow<PatientListUiState> = _listUiState

    // ── Patient Detail ──
    private val _detailUiState = MutableStateFlow(PatientDetailUiState())
    val detailUiState: StateFlow<PatientDetailUiState> = _detailUiState

    // ── Add Patient ──
    private val _addUiState = MutableStateFlow(AddPatientUiState())
    val addUiState: StateFlow<AddPatientUiState> = _addUiState

    private val _searchQuery = MutableStateFlow("")

    init {
        loadPatients()
    }

    private fun loadPatients() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) patientRepository.getAllPatients()
                    else patientRepository.searchPatients(query)
                }
                .collect { patients ->
                    _listUiState.value = PatientListUiState(
                        patients = patients,
                        searchQuery = _searchQuery.value,
                        isLoading = false
                    )
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _listUiState.value = _listUiState.value.copy(searchQuery = query)
    }

    fun loadPatientDetail(patientId: String) {
        viewModelScope.launch {
            _detailUiState.value = PatientDetailUiState(isLoading = true)
            val patient = patientRepository.getPatientById(patientId)
            _detailUiState.value = PatientDetailUiState(patient = patient, isLoading = false)
        }
    }

    // Add Patient field updates
    fun updateName(v: String) { _addUiState.value = _addUiState.value.copy(name = v) }
    fun updateAge(v: String) { _addUiState.value = _addUiState.value.copy(age = v) }
    fun updateGender(v: String) { _addUiState.value = _addUiState.value.copy(gender = v) }
    fun updateVillage(v: String) { _addUiState.value = _addUiState.value.copy(village = v) }
    fun updatePhone(v: String) { _addUiState.value = _addUiState.value.copy(phone = v) }
    fun updateAadhar(v: String) { _addUiState.value = _addUiState.value.copy(aadhar = v) }

    fun isAddPatientFormValid(): Boolean {
        val s = _addUiState.value
        return s.name.isNotBlank() && s.age.isNotBlank()
    }

    fun savePatient() {
        val state = _addUiState.value
        if (!isAddPatientFormValid()) {
            _addUiState.value = state.copy(error = "Name and age are required")
            return
        }
        viewModelScope.launch {
            _addUiState.value = state.copy(isSaving = true, error = null)
            val patient = Patient(
                id = UUID.randomUUID().toString(),
                name = state.name.trim(),
                age = state.age.toIntOrNull(),
                gender = state.gender,
                village = state.village.trim().ifBlank { null },
                phone = state.phone.trim().ifBlank { null },
                aadharNumber = state.aadhar.trim().ifBlank { null }
            )
            val result = patientRepository.savePatient(patient)
            if (result.isSuccess) {
                _addUiState.value = _addUiState.value.copy(isSaving = false, savedPatientId = patient.id)
            } else {
                _addUiState.value = _addUiState.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save patient"
                )
            }
        }
    }

    fun clearError() {
        _addUiState.value = _addUiState.value.copy(error = null)
        _detailUiState.value = _detailUiState.value.copy(error = null)
    }
}
