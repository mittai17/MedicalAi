package com.swasthai.app.feature.citizen.screening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.engine.AIEngineManager
import com.swasthai.app.ai.engine.ScanType
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.DiagnosisResult
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Screening
import com.swasthai.app.domain.model.ScreeningStatus
import com.swasthai.app.domain.model.ScreeningType
import com.swasthai.app.domain.model.Symptom
import com.swasthai.app.domain.model.SymptomSource
import com.swasthai.app.domain.model.Vitals
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Represents the current step in the screening wizard.
 */
enum class ScreeningStep {
    SYMPTOM_SELECTION,
    DURATION_SELECTION,
    VITALS_INPUT,
    PROCESSING,
    RESULT
}

/**
 * Predefined symptoms the user can select.
 */
data class SelectableSymptom(
    val name: String,
    val isSelected: Boolean = false
)

/**
 * UI state for the entire screening flow.
 */
data class ScreeningUiState(
    val currentStep: ScreeningStep = ScreeningStep.SYMPTOM_SELECTION,
    val screeningId: String = UUID.randomUUID().toString(),
    val screeningType: ScreeningType = ScreeningType.SYMPTOM_CHECK,

    // Step 1 — Symptoms
    val availableSymptoms: List<SelectableSymptom> = defaultSymptoms(),
    val otherSymptomText: String = "",

    // Step 2 — Duration
    val selectedDuration: String? = null,

    // Step 3 — Vitals
    val temperature: String = "",
    val pulse: String = "",
    val spo2: String = "",
    val bloodPressureSystolic: String = "",
    val bloodPressureDiastolic: String = "",
    val weight: String = "",
    val height: String = "",

    // Voice assistant
    val isRecording: Boolean = false,
    val voiceTranscript: String = "",

    // Image check
    val capturedImagePath: String? = null,
    val scanType: ScanType = ScanType.PNEUMONIA,

    // Result
    val diagnosisResult: DiagnosisResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private fun defaultSymptoms() = listOf(
    SelectableSymptom("Fever"),
    SelectableSymptom("Cough"),
    SelectableSymptom("Body Aches"),
    SelectableSymptom("Fatigue / Weakness"),
    SelectableSymptom("Headache"),
    SelectableSymptom("Sore Throat"),
    SelectableSymptom("Nausea / Vomiting"),
    SelectableSymptom("Diarrhea"),
    SelectableSymptom("Skin Rash"),
    SelectableSymptom("Difficulty Breathing")
)

/**
 * ViewModel managing the complete Citizen screening flow.
 *
 * Steps: Symptom Selection → Duration → Vitals → AI Processing → Result
 *
 * Supports symptom check, voice assistant, and image check flows.
 */
@HiltViewModel
class ScreeningViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    private val aiEngineManager: AIEngineManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreeningUiState())
    val uiState: StateFlow<ScreeningUiState> = _uiState

    // ── Symptom Selection ──

    fun toggleSymptom(symptomName: String) {
        _uiState.update { state ->
            state.copy(
                availableSymptoms = state.availableSymptoms.map {
                    if (it.name == symptomName) it.copy(isSelected = !it.isSelected) else it
                }
            )
        }
    }

    fun updateOtherSymptomText(text: String) {
        _uiState.update { it.copy(otherSymptomText = text) }
    }

    // ── Duration Selection ──

    fun selectDuration(duration: String) {
        _uiState.update { it.copy(selectedDuration = duration) }
    }

    // ── Vitals Input ──

    fun updateTemperature(value: String) { _uiState.update { it.copy(temperature = value) } }
    fun updatePulse(value: String) { _uiState.update { it.copy(pulse = value) } }
    fun updateSpo2(value: String) { _uiState.update { it.copy(spo2 = value) } }
    fun updateBPSystolic(value: String) { _uiState.update { it.copy(bloodPressureSystolic = value) } }
    fun updateBPDiastolic(value: String) { _uiState.update { it.copy(bloodPressureDiastolic = value) } }
    fun updateWeight(value: String) { _uiState.update { it.copy(weight = value) } }
    fun updateHeight(value: String) { _uiState.update { it.copy(height = value) } }

    // ── Voice assistant ──

    fun updateVoiceTranscript(text: String) {
        _uiState.update { it.copy(voiceTranscript = text) }
    }

    fun setRecording(recording: Boolean) {
        _uiState.update { it.copy(isRecording = recording) }
    }

    // ── Image check ──

    fun setCapturedImagePath(path: String?) {
        _uiState.update { it.copy(capturedImagePath = path) }
    }

    fun setScanType(scanType: ScanType) {
        _uiState.update { it.copy(scanType = scanType) }
    }

    // ── Navigation ──

    fun goToNextStep() {
        _uiState.update { state ->
            when (state.currentStep) {
                ScreeningStep.SYMPTOM_SELECTION -> state.copy(currentStep = ScreeningStep.DURATION_SELECTION)
                ScreeningStep.DURATION_SELECTION -> state.copy(currentStep = ScreeningStep.VITALS_INPUT)
                ScreeningStep.VITALS_INPUT -> {
                    runDiagnosis()
                    state.copy(currentStep = ScreeningStep.PROCESSING)
                }
                else -> state
            }
        }
    }

    fun goToPreviousStep() {
        _uiState.update { state ->
            when (state.currentStep) {
                ScreeningStep.DURATION_SELECTION -> state.copy(currentStep = ScreeningStep.SYMPTOM_SELECTION)
                ScreeningStep.VITALS_INPUT -> state.copy(currentStep = ScreeningStep.DURATION_SELECTION)
                else -> state
            }
        }
    }

    fun setScreeningType(type: ScreeningType) {
        _uiState.update { it.copy(screeningType = type) }
    }

    fun hasSelectedSymptoms(): Boolean {
        val state = _uiState.value
        return state.availableSymptoms.any { it.isSelected } || state.otherSymptomText.isNotBlank()
    }

    // ── Diagnosis ──

    private fun runDiagnosis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val state = _uiState.value
                val userId = userPreferences.userIdFlow.first() ?: ""

                // Build symptom list from selections
                val symptoms = buildSymptomList(state)

                // Build vitals from input
                val vitals = buildVitals(state)

                // Create screening record
                val screening = Screening(
                    id = state.screeningId,
                    userId = userId,
                    screeningType = state.screeningType,
                    status = ScreeningStatus.IN_PROGRESS,
                    symptoms = symptoms,
                    vitals = vitals
                )
                screeningRepository.createScreening(screening)

                // Save symptoms
                if (symptoms.isNotEmpty()) {
                    screeningRepository.saveSymptoms(symptoms)
                }

                // Save vitals
                if (vitals != null) {
                    screeningRepository.saveVitals(vitals)
                }

                // Run AI diagnosis
                val result = aiEngineManager.runDiagnosis(
                    symptoms = symptoms,
                    vitals = vitals,
                    imagePath = state.capturedImagePath,
                    voiceTranscript = state.voiceTranscript.ifBlank { null },
                    screeningId = state.screeningId,
                    scanType = state.scanType
                )

                // Save diagnosis result
                screeningRepository.saveDiagnosisResult(result)

                // Update screening as completed
                screeningRepository.updateScreening(
                    screening.copy(
                        status = ScreeningStatus.COMPLETED,
                        diagnosisResult = result
                    )
                )

                _uiState.update {
                    it.copy(
                        currentStep = ScreeningStep.RESULT,
                        diagnosisResult = result,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Diagnosis failed. Please try again.",
                        currentStep = ScreeningStep.VITALS_INPUT
                    )
                }
            }
        }
    }

    /**
     * Skip vitals and go directly to diagnosis.
     */
    fun skipVitalsAndDiagnose() {
        _uiState.update { it.copy(currentStep = ScreeningStep.PROCESSING) }
        runDiagnosis()
    }

    private fun buildSymptomList(state: ScreeningUiState): List<Symptom> {
        val symptoms = mutableListOf<Symptom>()

        // From checkbox selections
        state.availableSymptoms
            .filter { it.isSelected }
            .forEach { symptom ->
                symptoms.add(
                    Symptom(
                        id = UUID.randomUUID().toString(),
                        screeningId = state.screeningId,
                        name = symptom.name,
                        duration = state.selectedDuration,
                        source = SymptomSource.MANUAL
                    )
                )
            }

        // From "other" text input
        if (state.otherSymptomText.isNotBlank()) {
            symptoms.add(
                Symptom(
                    id = UUID.randomUUID().toString(),
                    screeningId = state.screeningId,
                    name = state.otherSymptomText.trim(),
                    duration = state.selectedDuration,
                    source = SymptomSource.MANUAL
                )
            )
        }

        // From voice transcript (simple extraction)
        if (state.voiceTranscript.isNotBlank()) {
            symptoms.add(
                Symptom(
                    id = UUID.randomUUID().toString(),
                    screeningId = state.screeningId,
                    name = state.voiceTranscript.trim(),
                    duration = state.selectedDuration,
                    source = SymptomSource.VOICE
                )
            )
        }

        return symptoms
    }

    private fun buildVitals(state: ScreeningUiState): Vitals? {
        val hasAnyVital = state.temperature.isNotBlank() ||
            state.pulse.isNotBlank() ||
            state.spo2.isNotBlank() ||
            state.bloodPressureSystolic.isNotBlank()

        if (!hasAnyVital) return null

        val bp = if (state.bloodPressureSystolic.isNotBlank() && state.bloodPressureDiastolic.isNotBlank()) {
            "${state.bloodPressureSystolic}/${state.bloodPressureDiastolic}"
        } else null

        return Vitals(
            id = UUID.randomUUID().toString(),
            screeningId = state.screeningId,
            temperature = state.temperature.toFloatOrNull(),
            pulse = state.pulse.toIntOrNull(),
            spo2 = state.spo2.toFloatOrNull(),
            bloodPressure = bp,
            weight = state.weight.toFloatOrNull(),
            height = state.height.toFloatOrNull()
        )
    }

    /**
     * Reset the screening flow for a new session.
     */
    fun resetScreening() {
        _uiState.value = ScreeningUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
