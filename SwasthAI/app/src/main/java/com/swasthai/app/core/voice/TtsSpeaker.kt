package com.swasthai.app.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide text-to-speech speaker for the voice command hub.
 *
 * Spoken slowly (0.8x rate) for elderly users. Initialization is lazy and
 * asynchronous; utterances spoken before the engine is ready are queued
 * and flushed once the engine initializes. TTS is optional — the hub
 * always shows the reply as text too, so a device without a TTS engine
 * still works.
 */
@Singleton
class TtsSpeaker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lock = Any()
    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)
    private var pending: Pair<String, String>? = null

    fun speak(text: String, languageCode: String = "en") {
        if (text.isBlank()) return
        synchronized(lock) {
            val engine = tts
            if (ready.get() && engine != null) {
                speakNow(engine, text, languageCode)
                return
            }
            pending = text to languageCode
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext) { status ->
                    synchronized(lock) {
                        if (status == TextToSpeech.SUCCESS) {
                            ready.set(true)
                            pending?.let { (queuedText, queuedLang) ->
                                pending = null
                                speakNow(tts!!, queuedText, queuedLang)
                            }
                        } else {
                            // No TTS engine available — the UI text is the reply.
                            pending = null
                            tts?.shutdown()
                            tts = null
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            runCatching { tts?.stop() }
        }
    }

    private fun speakNow(engine: TextToSpeech, text: String, languageCode: String) {
        runCatching {
            val locale = ttsLocaleFor(languageCode)
            if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                engine.setLanguage(locale)
            } else {
                engine.setLanguage(Locale.US)
            }
            engine.setSpeechRate(0.8f)
            engine.setPitch(1.0f)
            engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "swasthai-${System.currentTimeMillis()}"
            )
        }
    }
}