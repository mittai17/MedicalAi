package com.swasthai.app.feature.citizen.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.engine.AIEngineManager
import com.swasthai.app.ai.engine.ScanType
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.DiagnosisResult
import com.swasthai.app.domain.model.PatientContext
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Screening
import com.swasthai.app.domain.model.ScreeningStatus
import com.swasthai.app.domain.model.ScreeningType
import com.swasthai.app.domain.model.Symptom
import com.swasthai.app.domain.model.SymptomSource
import com.swasthai.app.domain.model.Vitals
import com.swasthai.app.domain.repository.ScreeningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

// ── Shared chat types ──

enum class ChatRole { USER, AI }

/** How a message's [ChatMessage.options] should be rendered in the chat. */
enum class OptionsMode {
    NONE,
    /** Multi-select symptom chips (Step 1 of the guided check). */
    MULTI_SYMPTOM,
    /** Single-select duration chips (Step 2). */
    SINGLE_DURATION,
    /** Vitals source choice, Step 3 (profile vs manual entry). */
    VITALS_SOURCE,
    /** Scan-type chips for an attached image. */
    SCAN_TYPE
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String = "",
    val imagePath: String? = null,
    val options: List<String> = emptyList(),
    val optionsMode: OptionsMode = OptionsMode.NONE,
    val streaming: Boolean = false,
    val screeningId: String? = null,
    val result: DiagnosisResult? = null
)

data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isRecording: Boolean = false,
    val isWorking: Boolean = false,
    val userName: String = "there",
    val vitalsTemperature: String = "",
    val vitalsPulse: String = "",
    val vitalsSpo2: String = "",
    val showVitalsInput: Boolean = false,
    val pendingReportScreeningId: String? = null,
    /** Persistent chip options for the active questionnaire step (survives streamed confirmations). */
    val activeOptions: List<String> = emptyList(),
    val activeOptionsMode: OptionsMode = OptionsMode.NONE
)

private const val MAX_MESSAGES = 80

private val CHAT_SYMPTOM_OPTIONS = listOf(
    "Fever", "Cough", "Body Aches", "Fatigue / Weakness", "Headache",
    "Sore Throat", "Nausea / Vomiting", "Diarrhea", "Skin Rash", "Difficulty Breathing"
)

private val DURATION_OPTIONS = listOf(
    "Less than 1 day", "1 – 3 days", "3 – 7 days", "More than 7 days"
)

private val VITALS_SOURCE_OPTIONS = listOf("Use my profile data", "Enter vitals manually")

private val SCAN_OPTIONS = ScanType.entries.map { it.displayName }

private val RESULT_OPTIONS = listOf("Create PDF report", "Start a new check")

/** The guided questionnaire's current stage. */
private enum class QStep { SYMPTOMS, DURATION, VITALS_SOURCE, RUNNING }

/**
 * AI Doctor chat console.
 *
 * A bounded, streaming, offline conversational UI over the on-device RAG
 * engine plus a guided "Health Check" questionnaire (symptoms → duration →
 * vitals), reusing the same evidence-based clinical reasoning the screening
 * wizard uses. Voice, image attach and full screening persistence are built
 * in. Conversation working memory is capped (LRU) to keep peak usage tiny.
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val chatEngine: AiChatEngine,
    private val aiEngineManager: AIEngineManager,
    private val screeningRepository: ScreeningRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState

    private var step: QStep? = null
    private val selectedSymptoms = mutableListOf<String>()
    private var duration: String? = null
    private var activeScreeningId: String? = null
    private var pendingImagePath: String? = null

    init {
        viewModelScope.launch {
            val name = userPreferences.userNameFlow.first() ?: "there"
            _uiState.update { it.copy(userName = name) }
            addAi(chatEngine.responseFor("hi"), streaming = true)
        }
    }

    // ── Input / voice ──

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setRecording(recording: Boolean) {
        _uiState.update { it.copy(isRecording = recording) }
    }

    /** Transcript/partial text from the voice recognizer lands in the input. */
    fun onVoiceText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /** Sends the typed message, or routes a chip-tapped skill as a message. */
    fun sendText(raw: String? = null) {
        val text = (raw ?: _uiState.value.inputText).trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        routeUserMessage(text)
    }

    private fun routeUserMessage(text: String) {
        addUser(text)
        when (val active = step) {
            QStep.SYMPTOMS -> {
                selectedSymptoms.add(text)
                addAi("Added “$text” ✓ — tap more symptoms below or choose Next → when done.", streaming = true)
            }
            QStep.DURATION -> {
                duration = text
                askVitalsSource()
            }
            QStep.VITALS_SOURCE -> {
                // Treated as "manual".
                _uiState.update { it.copy(showVitalsInput = true) }
                addAi("Alright — enter your latest readings below (all optional).📝", streaming = true)
            }
            else -> if (text.isNotBlank()) handleGeneral(text)
        }
    }

    // ── Skill chips ──

    fun onSkillTap(skill: AiSkill) {
        when (skill) {
            AiSkill.HEALTH_CHECK -> sendText("Let's check my symptoms")
            else -> sendText("Help me with ${skill.label}")
        }
    }

    private fun handleGeneral(text: String) {
        viewModelScope.launch {
            val skill = chatEngine.detectSkill(text)
            if (skill == AiSkill.HEALTH_CHECK) {
                startQuestionnaire()
                return@launch
            }
            streamAnswer { chatEngine.responseFor(text) }
        }
    }

    // ── Questionnaire: drop-down guided health check ──

    fun startQuestionnaire() {
        step = QStep.SYMPTOMS
        activeScreeningId = UUID.randomUUID().toString()
        selectedSymptoms.clear()
        duration = null
        _uiState.update {
            it.copy(
                showVitalsInput = false,
                vitalsTemperature = "",
                vitalsPulse = "",
                vitalsSpo2 = "",
                activeOptions = CHAT_SYMPTOM_OPTIONS + listOf("Next →"),
                activeOptionsMode = OptionsMode.MULTI_SYMPTOM
            )
        }
        addAi(text = "Health Check — Step 1. What is your main problem? Select all that apply. 👇")
    }

    fun toggleSymptom(name: String) {
        if (name == "Next →") {
            nextFromSymptoms()
            return
        }
        if (step != QStep.SYMPTOMS) return
        if (selectedSymptoms.contains(name)) {
            selectedSymptoms.remove(name)
            addAi("Removed “$name”.", streaming = true)
        } else {
            selectedSymptoms.add(name)
            addAi("“$name” noted ✓ — anything else, or Next →?", streaming = true)
        }
    }

    private fun nextFromSymptoms() {
        if (selectedSymptoms.isEmpty()) {
            addAi("Please pick at least one symptom so I can help properly.", streaming = true)
            return
        }
        step = QStep.DURATION
        _uiState.update {
            it.copy(activeOptions = DURATION_OPTIONS, activeOptionsMode = OptionsMode.SINGLE_DURATION)
        }
        addAi(text = "Step 2. How long have you had these symptoms?")
    }

    fun selectDuration(option: String) {
        if (step != QStep.DURATION) return
        duration = option
        askVitalsSource()
    }

    private fun askVitalsSource() {
        step = QStep.VITALS_SOURCE
        _uiState.update {
            it.copy(activeOptions = VITALS_SOURCE_OPTIONS, activeOptionsMode = OptionsMode.VITALS_SOURCE)
        }
        addAi(
            text = "Step 3. Vitals — I can use your profile data, or you can enter your latest readings."
        )
    }

    fun selectVitalsSource(option: String) {
        if (step != QStep.VITALS_SOURCE) return
        _uiState.update { it.copy(activeOptions = emptyList(), activeOptionsMode = OptionsMode.NONE) }
        if (option.contains("Enter", ignoreCase = true) || option.contains("manual", ignoreCase = true)) {
            _uiState.update { it.copy(showVitalsInput = true) }
            addAi("Enter your latest readings below (all optional) or just hit Done to skip. 📝", streaming = true)
        } else {
            viewModelScope.launch {
                val p = userPreferences
                val age = p.userAgeFlow.first()
                val sex = p.userSexFlow.first()
                val facts = listOfNotNull(
                    age?.let { "age $it" },
                    sex?.let { it.lowercase() }
                )
                addAi(
                    "Using your profile: ${facts.joinToString(" · ").ifBlank { "basic profile" }} — no manual vitals. " +
                        "Analysing now… ⏳",
                    streaming = true
                )
                runSymptomDiagnosis(vitals = null)
            }
        }
    }

    fun updateVitals(temperature: String, pulse: String, spo2: String) {
        _uiState.update {
            it.copy(vitalsTemperature = temperature, vitalsPulse = pulse, vitalsSpo2 = spo2)
        }
    }

    fun confirmVitals() {
        if (step != QStep.VITALS_SOURCE) return
        val s = _uiState.value
        val vitals = buildVitals(
            temperature = s.vitalsTemperature,
            pulse = s.vitalsPulse,
            spo2 = s.vitalsSpo2
        )
        _uiState.update { it.copy(showVitalsInput = false) }
        viewModelScope.launch { runSymptomDiagnosis(vitals) }
    }

    fun attachImage(path: String) {
        pendingImagePath = path
        step = null
        _uiState.update {
            it.copy(
                showVitalsInput = false,
                activeOptions = SCAN_OPTIONS,
                activeOptionsMode = OptionsMode.SCAN_TYPE
            )
        }
        addUser("", imagePath = path)
        addAi(text = "Got the image 📷. On-device analysis — what type of scan is this?")
    }

    fun chooseScanType(displayName: String) {
        val scan = ScanType.entries.firstOrNull { it.displayName == displayName } ?: return
        val path = pendingImagePath ?: return
        pendingImagePath = null
        _uiState.update { it.copy(activeOptions = emptyList(), activeOptionsMode = OptionsMode.NONE) }
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            addAi("Analysing with the local vision model (${scan.displayName})… ⏳", streaming = true)
            try {
                val screeningId = UUID.randomUUID().toString()
                val userId = userPreferences.userIdFlow.first() ?: ""
                val screening = Screening(
                    id = screeningId,
                    userId = userId,
                    screeningType = ScreeningType.IMAGE_CHECK,
                    status = ScreeningStatus.IN_PROGRESS,
                    symptoms = emptyList(),
                    vitals = null
                )
                screeningRepository.createScreening(screening)
                val patientContext = patientContext()
                val result = aiEngineManager.runDiagnosis(
                    symptoms = emptyList(),
                    vitals = null,
                    imagePath = path,
                    screeningId = screeningId,
                    scanType = scan,
                    patientContext = patientContext
                )
                screeningRepository.saveDiagnosisResult(result)
                screeningRepository.updateScreening(
                    screening.copy(status = ScreeningStatus.COMPLETED, diagnosisResult = result)
                )
                presentResult(result, screeningId)
            } catch (e: Exception) {
                addAi("I couldn't analyse that image (${e.message}). Try a clearer photo or another scan type.", streaming = true)
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    // ── Diagnosis ──

    private suspend fun runSymptomDiagnosis(vitals: Vitals?) {
        _uiState.update { it.copy(isWorking = true) }
        try {
            val screeningId = activeScreeningId ?: UUID.randomUUID().toString()
            val userId = userPreferences.userIdFlow.first() ?: ""
            val symptoms = selectedSymptoms.map {
                Symptom(
                    id = UUID.randomUUID().toString(),
                    screeningId = screeningId,
                    name = it,
                    duration = duration,
                    source = SymptomSource.MANUAL
                )
            }
            val screening = Screening(
                id = screeningId,
                userId = userId,
                screeningType = ScreeningType.SYMPTOM_CHECK,
                status = ScreeningStatus.IN_PROGRESS,
                symptoms = symptoms,
                vitals = vitals
            )
            screeningRepository.createScreening(screening)
            if (symptoms.isNotEmpty()) screeningRepository.saveSymptoms(symptoms)
            if (vitals != null) screeningRepository.saveVitals(vitals)

            val result = aiEngineManager.runDiagnosis(
                symptoms = symptoms,
                vitals = vitals,
                imagePath = null,
                screeningId = screeningId,
                scanType = ScanType.PNEUMONIA,
                patientContext = patientContext()
            )
            screeningRepository.saveDiagnosisResult(result)
            screeningRepository.updateScreening(
                screening.copy(status = ScreeningStatus.COMPLETED, diagnosisResult = result)
            )
            presentResult(result, screeningId)
        } catch (e: Exception) {
            addAi("I hit an error while analysing (${e.message}). Please try again.", streaming = true)
        } finally {
            _uiState.update { it.copy(isWorking = false) }
            step = null
        }
    }

    private suspend fun patientContext(): PatientContext {
        val p = userPreferences
        return PatientContext(
            age = p.userAgeFlow.first(),
            sex = p.userSexFlow.first(),
            chronicConditions = p.userConditionsFlow.first()
        )
    }

    private fun presentResult(result: DiagnosisResult, screeningId: String) {
        val riskLabel = when (result.riskLevel) {
            RiskLevel.HIGH -> "High"
            RiskLevel.MODERATE -> "Moderate"
            RiskLevel.LOW -> "Low"
        }
        val topRec = result.recommendations.firstOrNull()?.text
        val advice = result.medicalAdvice
        val text = buildString {
            appendLine("Here is your result: 📊")
            appendLine("• Likely condition: ${result.predictedDisease}")
            appendLine("• Risk level: $riskLabel")
            appendLine("• Confidence: ${result.confidenceScore.toInt()}%")
            topRec?.let { appendLine("• First step: $it") }
            advice?.let { appendLine()
                appendLine("${it.cause}")
                appendLine("${it.remedy}") }
            appendLine()
            append("This was computed fully on-device. Want a shareable report, or a new check?")
        }
        addAi(
            text = text,
            options = RESULT_OPTIONS,
            screeningId = screeningId,
            result = result
        )
    }

    // ── Result actions ──

    fun onResultOption(option: String, screeningId: String?) {
        if (option.contains("report", ignoreCase = true) && screeningId != null) {
            _uiState.update { it.copy(pendingReportScreeningId = screeningId) }
        } else if (option.contains("new check", ignoreCase = true)) {
            startNewCheck()
        }
    }

    fun clearPendingReport() {
        _uiState.update { it.copy(pendingReportScreeningId = null) }
    }

    fun startNewCheck() {
        step = null
        activeScreeningId = null
        pendingImagePath = null
        _uiState.update {
            it.copy(
                showVitalsInput = false,
                vitalsTemperature = "",
                vitalsPulse = "",
                vitalsSpo2 = "",
                activeOptions = emptyList(),
                activeOptionsMode = OptionsMode.NONE
            )
        }
        viewModelScope.launch { streamAnswer { chatEngine.responseFor("hi") } }
    }

    /** Wipe the conversation (frees the working-memory buffer). */
    fun clearConversation() {
        step = null
        selectedSymptoms.clear()
        duration = null
        activeScreeningId = null
        pendingImagePath = null
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    showVitalsInput = false,
                    activeOptions = emptyList(),
                    activeOptionsMode = OptionsMode.NONE
                )
            }
            addAi(chatEngine.responseFor("hi"), streaming = true)
        }
    }

    // ── Streaming / memory ──

    private fun addUser(text: String, imagePath: String? = null) {
        appendMessage(ChatMessage(role = ChatRole.USER, text = text, imagePath = imagePath))
    }

    private fun addAi(
        text: String,
        options: List<String> = emptyList(),
        optionsMode: OptionsMode = OptionsMode.NONE,
        streaming: Boolean = false,
        screeningId: String? = null,
        result: DiagnosisResult? = null
    ) {
        if (streaming) {
            viewModelScope.launch { streamAnswer { text } }
            return
        }
        appendMessage(
            ChatMessage(
                role = ChatRole.AI,
                text = text,
                options = options,
                optionsMode = optionsMode,
                screeningId = screeningId,
                result = result
            )
        )
    }

    private suspend fun streamAnswer(answer: () -> String) {
        // Compute the answer off the main thread (RAG retrieval + scoring).
        val text = withContext(Dispatchers.Default) { answer() }
        val messageId = UUID.randomUUID().toString()
        appendMessage(ChatMessage(role = ChatRole.AI, id = messageId, text = "", streaming = true))
        val words = text.split(" ")
        val sb = StringBuilder()
        // Stream in small chunks with a very short tick for a fast "live" feel.
        var i = 0
        while (i < words.size) {
            val end = minOf(i + 2, words.size)
            if (i > 0) sb.append(' ')
            sb.append(words.subList(i, end).joinToString(" "))
            val partial = sb.toString().trim()
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { m ->
                        if (m.id == messageId) m.copy(text = partial, streaming = true) else m
                    }
                )
            }
            i = end
            delay(5L)
        }
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { m ->
                    if (m.id == messageId) m.copy(text = text, streaming = false) else m
                }
            )
        }
    }

    /**
     * Append, keeping the working memory bounded: old messages are trimmed
     * (LRU-style) so the conversation never grows unbounded.
     */
    private fun appendMessage(message: ChatMessage) {
        _uiState.update { state ->
            val trimmed = if (state.messages.size >= MAX_MESSAGES) {
                state.messages.drop(2)
            } else {
                state.messages
            }
            state.copy(messages = trimmed + message)
        }
    }

    private fun buildVitals(temperature: String, pulse: String, spo2: String): Vitals? {
        val hasAny = temperature.isNotBlank() || pulse.isNotBlank() || spo2.isNotBlank()
        if (!hasAny) return null
        return Vitals(
            id = UUID.randomUUID().toString(),
            screeningId = activeScreeningId ?: "",
            temperature = temperature.toFloatOrNull(),
            pulse = pulse.toIntOrNull(),
            spo2 = spo2.toFloatOrNull()
        )
    }
}