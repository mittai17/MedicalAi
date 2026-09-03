package com.swasthai.app.feature.citizen.screening

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Thin wrapper around Android's [SpeechRecognizer] used by the Voice
 * Assistant. Runs on the calling (main) thread — callbacks arrive there.
 *
 * Mirrors errors from Android's error codes into user-friendly text so the
 * screen can react (e.g. fall back to the keyboard when no service exists).
 */
class VoiceRecognizer(
    context: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(message: String)
    }

    private val speechRecognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }

    val isAvailable: Boolean get() = speechRecognizer != null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                .orEmpty()
            if (matches.isNotEmpty()) {
                callbacks.onPartialResult(matches.first())
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                .orEmpty()
            callbacks.onFinalResult(matches.firstOrNull() ?: "")
        }

        override fun onError(error: Int) {
            callbacks.onError(friendlyError(error))
        }
    }

    fun start(locale: Locale) {
        val recognizer = speechRecognizer ?: return
        recognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer.startListening(intent)
    }

    fun stop() {
        speechRecognizer?.stopListening()
    }

    fun cancel() {
        speechRecognizer?.cancel()
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }

    private fun friendlyError(error: Int) = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected — tap the mic and speak."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice input."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy — try again in a second."
        SpeechRecognizer.ERROR_AUDIO -> "Could not access the microphone."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Voice recognition needs a service — you can type your symptoms instead."
        SpeechRecognizer.ERROR_SERVER -> "Voice service error — try again or type instead."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "This language isn't supported for voice — use the keyboard."
        SpeechRecognizer.ERROR_CLIENT -> "Speech service unavailable — use the keyboard instead."
        else -> "Voice input failed — try again or type your symptoms."
    }
}