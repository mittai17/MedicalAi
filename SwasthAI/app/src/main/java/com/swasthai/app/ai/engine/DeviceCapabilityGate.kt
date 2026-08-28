package com.swasthai.app.ai.engine

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether this device can run the heavy on-device LLM fallback
 * (Gemma 4 E2B-IT).
 *
 * The on-device LLM stacks (LiteRT-LM / MediaPipe) do not ship 32-bit ARM
 * libraries and need enough RAM. Low-end / 32-bit devices therefore use the
 * on-device primary models (TFLite classifiers + reasoning + RAG) ONLY, and
 * never attempt to load the LLM.
 */
@Singleton
class DeviceCapabilityGate @Inject constructor() {

    /**
     * Deployment mode for this device:
     *  - [FULL]: capable device (arm64 + RAM) — runs Gemma 4 E2B-IT LLM.
     *  - [UI_ONLY]: low-end / 32-bit device — primary models + RAG + UI only,
     *    never loads the LLM.
     */
    enum class DeviceMode { FULL, UI_ONLY }

    fun currentMode(): DeviceMode =
        if (canRunOnDeviceLlm()) DeviceMode.FULL else DeviceMode.UI_ONLY

    /**
     * True only on 64-bit ARM devices with enough RAM to host a ~2GB model
     * alongside the OS. On all other devices the LLM fallback is disabled and
     * the app stays purely on-device.
     */
    fun canRunOnDeviceLlm(): Boolean {
        val isArm64 = Build.SUPPORTED_ABIS?.any {
            it.startsWith("arm64-v8a", ignoreCase = true)
        } == true
        if (!isArm64) return false

        // Require comfortably more RAM than the model footprint (~2-3GB).
        val totalMb = readMeminfoMb()
        return totalMb < 0 || totalMb >= 5_000
    }

    private fun readMeminfoMb(): Long {
        return try {
            val text = java.io.File("/proc/meminfo").readText()
            val line = text.lineSequence().firstOrNull { it.startsWith("MemTotal") } ?: return -1L
            val kb = Regex("\\d+").find(line)?.value?.toLongOrNull() ?: return -1L
            kb / 1024
        } catch (e: Throwable) {
            -1L
        }
    }
}
