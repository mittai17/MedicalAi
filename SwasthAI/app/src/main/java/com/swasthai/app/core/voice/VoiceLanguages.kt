package com.swasthai.app.core.voice

import java.util.Locale

/**
 * Speech languages supported by the voice command hub.
 *
 * [SpeechRecognizer] supports all of these for input; TextToSpeech falls
 * back to English when no engine is installed for the chosen language.
 */
data class VoiceLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
)

val VOICE_LANGUAGES = listOf(
    VoiceLanguage("en", "English", "English"),
    VoiceLanguage("hi", "हिन्दी", "Hindi"),
    VoiceLanguage("ta", "தமிழ்", "Tamil"),
    VoiceLanguage("te", "తెలుగు", "Telugu"),
    VoiceLanguage("kn", "ಕನ್ನಡ", "Kannada"),
    VoiceLanguage("ml", "മലയാളം", "Malayalam"),
    VoiceLanguage("or", "ଓଡ଼ିଆ", "Odia")
)

/** Locale for the speech recognizer (SpeechRecognizer EXTRA_LANGUAGE). */
fun speechLocaleFor(code: String): Locale = when (code) {
    "hi" -> Locale("hi", "IN")
    "ta" -> Locale("ta", "IN")
    "te" -> Locale("te", "IN")
    "kn" -> Locale("kn", "IN")
    "ml" -> Locale("ml", "IN")
    "or" -> Locale("or", "IN")
    else -> Locale.US
}

/** Locale for the text-to-speech engine. */
fun ttsLocaleFor(code: String): Locale = speechLocaleFor(code)

/** "English · English" style label for the language selector. */
fun voiceLanguageLabel(code: String): String {
    val lang = VOICE_LANGUAGES.firstOrNull { it.code == code } ?: return code
    return "${lang.nativeName} · ${lang.englishName}"
}