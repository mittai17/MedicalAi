package com.swasthai.app.feature.citizen.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.core.reminders.ReminderScheduler
import com.swasthai.app.core.voice.AwaitKind
import com.swasthai.app.core.voice.VOICE_LANGUAGES
import com.swasthai.app.core.voice.VoiceCommandParser
import com.swasthai.app.core.voice.VoiceIntent
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.domain.model.ConsultationRequest
import com.swasthai.app.domain.model.Reminder
import com.swasthai.app.domain.repository.ConsultationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the hub asks the UI to navigate. */
enum class VoiceNavTarget { CONNECT_PROVIDERS, HEALTH_RECORDS }

/** Actions the hub VM hands to the UI (which owns the shared ScreeningViewModel). */
enum class VoiceAction { RUN_SYMPTOM_CHECK, OPEN_CAMERA }

/**
 * A single follow-up question awaiting one more utterance.
 */
data class PendingDetail(
    val kind: AwaitKind,
    val seed: VoiceIntent? = null,
    val extra: String = ""
)

data class VoiceCommandUiState(
    val language: String = "en",
    val lastTranscript: String = "",
    val reply: String = "",
    val speakId: Int = 0,
    val pending: PendingDetail? = null,
    val navTarget: VoiceNavTarget? = null,
    val action: VoiceAction? = null,
    val actionPayload: String = ""
)

/**
 * Brain of the "Talk to SwasthAI" hub.
 *
 * Converts each utterance into a [VoiceIntent], executes the ones the UI
 * must drive (symptom check / camera via the shared ScreeningViewModel),
 * and side-effects the rest here: reminders are persisted + alarmed (with
 * recurrence), consultations are queued offline-first, and navigation
 * targets are surfaced as [VoiceCommandUiState.navTarget].
 */
@HiltViewModel
class VoiceCommandHubViewModel @Inject constructor(
    private val parser: VoiceCommandParser,
    private val userPreferences: UserPreferences,
    private val reminderScheduler: ReminderScheduler,
    private val consultationRepository: ConsultationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceCommandUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Default the assistant to the user's saved app language.
            val saved = userPreferences.languageFlow.first()
            val code = if (VOICE_LANGUAGES.any { it.code == saved }) saved else "en"
            _uiState.update { it.copy(language = code) }
        }
    }

    fun setLanguage(code: String) {
        _uiState.update { it.copy(language = code) }
    }

    // ── Entry point ──

    fun handleUtterance(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        // Don't accept a new command while the UI is still executing one.
        if (_uiState.value.action != null) return

        val pending = _uiState.value.pending
        if (pending != null) {
            resolvePending(pending, trimmed)
            return
        }

        val lang = _uiState.value.language
        val cmd = parser.parse(trimmed, lang)
        _uiState.update {
            it.copy(lastTranscript = trimmed, reply = cmd.reply, speakId = it.speakId + 1)
        }
        when (cmd.intent) {
            is VoiceIntent.HealthCheck -> {
                val symptoms = cmd.intent.symptoms
                if (symptoms.isNullOrBlank()) {
                    val prompt = parser.promptSymptoms(lang)
                    _uiState.update {
                        it.copy(
                            pending = PendingDetail(AwaitKind.AWAITING_SYMPTOMS),
                            reply = prompt,
                            speakId = it.speakId + 1
                        )
                    }
                } else {
                    startSymptomCheck(symptoms)
                }
            }

            is VoiceIntent.StartCamera -> {
                _uiState.update {
                    it.copy(
                        action = VoiceAction.OPEN_CAMERA,
                        actionPayload = cmd.intent.scanType.name
                    )
                }
            }

            is VoiceIntent.SetReminder -> handleSetReminder(cmd.intent)

            is VoiceIntent.BookConsultation -> {
                val reason = cmd.intent.reason
                if (reason.isBlank() || reason == "Requested via voice assistant") {
                    val prompt = parser.promptConsultationReason(lang)
                    _uiState.update {
                        it.copy(
                            pending = PendingDetail(
                                kind = AwaitKind.AWAITING_CONSULTATION_REASON,
                                extra = cmd.intent.urgency
                            ),
                            reply = prompt,
                            speakId = it.speakId + 1
                        )
                    }
                } else {
                    submitConsultation(reason, cmd.intent.urgency)
                }
            }

            VoiceIntent.ShowReminders -> loadReminders()

            VoiceIntent.FindCare -> {
                _uiState.update { it.copy(navTarget = VoiceNavTarget.CONNECT_PROVIDERS) }
            }

            VoiceIntent.ShowRecords -> {
                _uiState.update { it.copy(navTarget = VoiceNavTarget.HEALTH_RECORDS) }
            }

            VoiceIntent.Emergency -> {
                _uiState.update { it.copy(navTarget = VoiceNavTarget.CONNECT_PROVIDERS) }
            }

            VoiceIntent.Help, VoiceIntent.Greeting, VoiceIntent.Cancel -> Unit
            is VoiceIntent.Unknown -> Unit // reply already set above

            else -> Unit
        }
    }

    // ── Follow-up dialog resolution ──

    private fun resolvePending(pending: PendingDetail, text: String) {
        val lang = _uiState.value.language
        when (pending.kind) {
            AwaitKind.AWAITING_SYMPTOMS -> startSymptomCheck(text)

            AwaitKind.AWAITING_REMINDER_TIME -> {
                val seed = (pending.seed as? VoiceIntent.SetReminder)
                    ?: VoiceIntent.SetReminder("Health reminder", null, null)
                val repeat = parser.parseRepeatInterval(text, lang) ?: seed.repeatIntervalMillis
                val time = parser.parseTimeOfDay(text, lang)
                if (time == null && repeat == null) {
                    val prompt = parser.promptReminderTime(lang)
                    _uiState.update {
                        it.copy(
                            reply = prompt,
                            speakId = it.speakId + 1,
                            lastTranscript = text
                        )
                    }
                    return
                }
                handleSetReminder(
                    VoiceIntent.SetReminder(seed.title, time ?: seed.timeOfDayMillis, repeat)
                )
            }

            AwaitKind.AWAITING_CONSULTATION_REASON -> {
                val urgency = pending.extra.ifBlank { "NORMAL" }
                if (text.isNotBlank()) submitConsultation(text, urgency)
            }

            else -> Unit
        }
    }

    // ── Symptom check (executed by the UI via the shared ScreeningViewModel) ──

    private fun startSymptomCheck(symptoms: String) {
        _uiState.update {
            it.copy(
                pending = null,
                action = VoiceAction.RUN_SYMPTOM_CHECK,
                actionPayload = symptoms,
                lastTranscript = symptoms
            )
        }
    }

    // ── Reminders (persisted + alarmed, with recurrence) ──

    private fun handleSetReminder(intent: VoiceIntent.SetReminder) {
        val lang = _uiState.value.language
        val repeat = intent.repeatIntervalMillis
        var timeOfDay = intent.timeOfDayMillis
        // Daily reminders default to 9 AM when no time was spoken.
        if (timeOfDay == null && repeat == VoiceCommandParser.DAY) {
            timeOfDay = 9L * VoiceCommandParser.HOUR
        }

        when {
            timeOfDay != null -> viewModelScope.launch {
                val title = intent.title
                scheduleReminder(title, nextOccurrenceOf(timeOfDay), repeat)
                val timeText = timeTextOfDay(timeOfDay)
                val repeatText = repeatLabel(repeat, lang)
                val reply = parser.confirmedReminder(lang, title, timeText, repeatText)
                _uiState.update { it.copy(reply = reply, speakId = it.speakId + 1) }
            }

            repeat != null -> viewModelScope.launch {
                val title = intent.title
                scheduleReminder(title, System.currentTimeMillis() + repeat, repeat)
                val reply = parser.confirmedReminder(
                    lang, title, null, repeatLabel(repeat, lang)
                )
                _uiState.update { it.copy(reply = reply, speakId = it.speakId + 1) }
            }

            else -> {
                val prompt = parser.promptReminderTime(lang)
                _uiState.update {
                    it.copy(
                        pending = PendingDetail(
                            kind = AwaitKind.AWAITING_REMINDER_TIME,
                            seed = intent
                        ),
                        reply = prompt,
                        speakId = it.speakId + 1
                    )
                }
            }
        }
    }

    private suspend fun scheduleReminder(title: String, timeMillis: Long, repeat: Long?) {
        if (timeMillis <= 0L) return
        val reminder = Reminder(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Health reminder" },
            timeInMillis = timeMillis,
            repeatIntervalMillis = repeat ?: 0L,
            createdAt = System.currentTimeMillis()
        )
        userPreferences.addReminder(reminder)
        reminderScheduler.schedule(reminder)
    }

    private fun loadReminders() {
        val lang = _uiState.value.language
        viewModelScope.launch {
            val reminders = userPreferences.remindersFlow.first()
            val reply = if (reminders.isEmpty()) {
                parser.remindersEmptyReply(lang)
            } else {
                val summary = reminders
                    .take(5)
                    .joinToString("; ") { "${it.title} at ${timeTextAbsolute(it.timeInMillis)}" }
                parser.remindersSummaryReply(lang, summary)
            }
            _uiState.update { it.copy(reply = reply, speakId = it.speakId + 1) }
        }
    }

    // ── Consultation booking (offline-first queue) ──

    private fun submitConsultation(reason: String, urgency: String) {
        val lang = _uiState.value.language
        viewModelScope.launch {
            val userId =
                userPreferences.userIdFlow.first() ?: userPreferences.stableUserId()
            val result = consultationRepository.submitRequest(
                ConsultationRequest(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    reason = reason.trim(),
                    urgency = urgency,
                    patientName = "Self",
                    createdAt = System.currentTimeMillis()
                )
            )
            val reply = result.fold(
                onSuccess = { parser.confirmedConsultation(lang) },
                onFailure = { parser.consultationFailed(lang) }
            )
            _uiState.update {
                it.copy(
                    pending = null,
                    reply = reply,
                    speakId = it.speakId + 1
                )
            }
        }
    }

    // ── UI consumption helpers ──

    fun consumeAction() {
        _uiState.update { it.copy(action = null, actionPayload = "") }
    }

    fun clearNavTarget() {
        _uiState.update { it.copy(navTarget = null) }
    }

    fun resetPending() {
        _uiState.update { it.copy(pending = null) }
    }

    // ── Time helpers ──

    /** Next occurrence of this time-of-day (today if still ahead, else tomorrow). */
    private fun nextOccurrenceOf(timeOfDayMillis: Long): Long {
        val cal = Calendar.getInstance()
        val hour = (timeOfDayMillis / VoiceCommandParser.HOUR).toInt()
        val minute =
            ((timeOfDayMillis % VoiceCommandParser.HOUR) / VoiceCommandParser.MINUTE).toInt()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var millis = cal.timeInMillis
        if (millis <= System.currentTimeMillis()) {
            millis += VoiceCommandParser.DAY
        }
        return millis
    }

    fun timeTextOfDay(timeOfDayMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, (timeOfDayMillis / VoiceCommandParser.HOUR).toInt())
        cal.set(
            Calendar.MINUTE,
            ((timeOfDayMillis % VoiceCommandParser.HOUR) / VoiceCommandParser.MINUTE).toInt()
        )
        cal.set(Calendar.SECOND, 0)
        return SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
    }

    fun timeTextAbsolute(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))

    private fun repeatLabel(repeat: Long?, lang: String): String {
        if (repeat == null || repeat <= 0L) return ""
        val dayPart = if (lang == "hi") "har din" else "every day"
        val hoursPart = if (lang == "hi") "har" else "every"
        val theHour = if (lang == "hi") "ghante" else "hours"
        val theMinute = if (lang == "hi") "minute" else "minutes"
        return when {
            repeat == VoiceCommandParser.DAY -> dayPart
            repeat % VoiceCommandParser.DAY == 0L ->
                "$hoursPart ${(repeat / VoiceCommandParser.DAY)} ${if (lang == "hi") "din" else "days"}"
            repeat % VoiceCommandParser.HOUR == 0L ->
                "$hoursPart ${(repeat / VoiceCommandParser.HOUR)} $theHour"
            repeat % VoiceCommandParser.MINUTE == 0L ->
                "$hoursPart ${(repeat / VoiceCommandParser.MINUTE)} $theMinute"
            else -> dayPart
        }
    }
}