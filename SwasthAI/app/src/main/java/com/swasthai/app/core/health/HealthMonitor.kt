package com.swasthai.app.core.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.swasthai.app.data.local.datastore.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room for a single live health snapshot for the dashboard widgets.
 *
 * Reads real device sensors where present:
 *  - Step count via [Sensor.TYPE_STEP_COUNTER] (delta since monitoring
 *    started) or [Sensor.TYPE_STEP_DETECTOR] (accumulated detections).
 *  - Heart rate via [Sensor.TYPE_HEART_RATE].
 *
 * When a sensor is not available on the device (deep emulators, older
 * phones) the widgets fall back to persisted manual values entered by the
 * user. Sensor values always win over manual ones while live.
 *
 * Memory impact is negligible — no model, no buffers; listeners push tiny
 * primitive updates.
 */
@Singleton
class HealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {

    /** Current snapshot shown by the dashboard widgets. */
    data class HealthState(
        val steps: Int = 0,
        val heartRate: Int? = null,
        val stepsLive: Boolean = false,
        val heartLive: Boolean = false,
        val hardwareSupported: Boolean = false
    )

    private val _state = MutableStateFlow(HealthState())
    val state: StateFlow<HealthState> = _state

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var counterAvailable = false
    private var heartAvailable = false

    // Live sensor deltas/values.
    @Volatile private var stepCounterBaseline: Float? = null
    @Volatile private var stepCounterCum: Float? = null
    @Volatile private var detectorSteps = 0
    @Volatile private var liveHeart: Int? = null

    // Persisted manual fallbacks.
    @Volatile private var manualSteps = 0
    @Volatile private var manualHeart = 0

    // Persisted live step-counter anchor (today's baseline).
    @Volatile private var currentBaselineDay = ""
    @Volatile private var currentBaselineValue = 0f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val stepCounterListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val value = event?.values?.firstOrNull() ?: return
            val today = todayString()
            val current = currentBaselineDay
            val baseline = if (current != today) {
                currentBaselineDay = today
                currentBaselineValue = value
                scope.launch { userPreferences.setSensorBaseline(today, value) }
                value
            } else {
                currentBaselineValue
            }
            stepCounterCum = (value - baseline).coerceAtLeast(0f)
            publish()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val stepDetectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if ((event?.values?.firstOrNull() ?: 0f) > 0f) {
                detectorSteps++
                publish()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val heartListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val value = event?.values?.firstOrNull()?.toInt() ?: return
            if (value in 20..300) {
                liveHeart = value
                publish()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        // Restore today's step anchor so counts survive app restarts.
        scope.launch {
            currentBaselineDay = userPreferences.sensorBaselineDayFlow.first()
            currentBaselineValue = userPreferences.sensorBaselineValueFlow.first()
            if (currentBaselineDay != todayString()) {
                currentBaselineDay = ""
                currentBaselineValue = 0f
            }
            publish()
        }

        // Manual fallbacks stream in from DataStore.
        scope.launch { userPreferences.manualStepsFlow.collect { manualSteps = it; publish() } }
        scope.launch { userPreferences.manualHeartRateFlow.collect { manualHeart = it; publish() } }

        // Register the lightest available step sensor.
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        when {
            stepCounter != null -> {
                counterAvailable = true
                sensorManager.registerListener(
                    stepCounterListener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            stepDetector != null -> {
                counterAvailable = true
                sensorManager.registerListener(
                    stepDetectorListener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }

        val heart = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (heart != null) {
            heartAvailable = true
            sensorManager.registerListener(heartListener, heart, SensorManager.SENSOR_DELAY_NORMAL)
        }

        publish()
    }

    /** Store a manually entered step count (used when no live sensor exists). */
    suspend fun setManualSteps(steps: Int) = userPreferences.setManualSteps(steps)

    /** Store a manually entered heart rate (used when no live sensor exists). */
    suspend fun setManualHeartRate(bpm: Int) = userPreferences.setManualHeartRate(bpm)

    private fun publish() {
        val steps = when {
            counterAvailable -> stepCounterCum?.toInt() ?: detectorSteps
            else -> manualSteps
        }
        val heart = liveHeart ?: (manualHeart.takeIf { it > 0 } ?: null)

        _state.value = HealthState(
            steps = steps,
            heartRate = heart,
            stepsLive = counterAvailable,
            heartLive = liveHeart != null,
            hardwareSupported = counterAvailable || heartAvailable
        )
    }
}