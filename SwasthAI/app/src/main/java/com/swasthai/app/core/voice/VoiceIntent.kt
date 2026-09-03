package com.swasthai.app.core.voice

import com.swasthai.app.ai.engine.ScanType

/**
 * A parsed user request, produced by [VoiceCommandParser].
 *
 * The hub executes the intent — navigation, a diagnosis run, a persisted
 * reminder, a consultation submission — and speaks [reply]. Some intents
 * (those with `null` detail) leave the hub in a follow-up dialog to ask
 * for the missing piece (symptoms, reminder time, consultation reason).
 */
sealed interface VoiceIntent {

    /** Spoken symptom check. [symptoms] is the free-text symptom phrase. */
    data class HealthCheck(val symptoms: String?) : VoiceIntent

    /** Open the camera and scan a photo of the given type. */
    data class StartCamera(val scanType: ScanType) : VoiceIntent

    /** Persist a reminder. Time is millis-of-day; repeat is full millis. */
    data class SetReminder(
        val title: String,
        val timeOfDayMillis: Long?,
        val repeatIntervalMillis: Long?
    ) : VoiceIntent

    /** Submit an online consultation request. */
    data class BookConsultation(val reason: String, val urgency: String) : VoiceIntent

    data object ShowReminders : VoiceIntent
    data object FindCare : VoiceIntent
    data object ShowRecords : VoiceIntent
    data object Emergency : VoiceIntent
    data object Help : VoiceIntent
    data object Greeting : VoiceIntent
    data object Cancel : VoiceIntent

    data class Unknown(val text: String) : VoiceIntent
}

/**
 * Result of parsing an utterance: the matched intent plus the reply text
 * the assistant should speak/show to the user.
 */
data class ParsedVoiceCommand(
    val intent: VoiceIntent,
    val reply: String
)

/**
 * A follow-up question awaiting one more utterance before an action can
 * complete (e.g. symptoms, reminder time, consultation reason).
 */
enum class AwaitKind {
    AWAITING_SYMPTOMS,
    AWAITING_REMINDER_TIME,
    AWAITING_CONSULTATION_REASON
}