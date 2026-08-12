package com.swasthai.app.ai.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and manages TensorFlow Lite models from the assets directory.
 *
 * In production, this loads actual .tflite files. Currently uses mock
 * implementations that can be swapped by dropping real model files
 * into the assets/ directory.
 *
 * Models:
 * - whisper_tiny.tflite — Voice → Speech-to-Text
 * - mobilenet_v3.tflite — Image Classification
 * - symptom_predictor.tflite — Symptom → Disease Prediction
 */
@Singleton
class ModelLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check if a model file exists in assets.
     */
    fun isModelAvailable(modelName: String): Boolean {
        return try {
            context.assets.open(modelName).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Load model bytes from assets.
     */
    fun loadModelBytes(modelName: String): ByteArray? {
        return try {
            context.assets.open(modelName).use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val MODEL_WHISPER = "whisper_tiny.tflite"
        const val MODEL_SYMPTOM = "symptom_predictor.tflite"

        // Screening models (28x28 inputs, FP16 quantized)
        const val MODEL_PNEUMONIA = "mobilenet_v3.tflite"
        const val MODEL_BREAST_SCAN = "breast_scan.tflite"
        const val MODEL_RETINA_DR = "retina_dr.tflite"
        const val MODEL_SKIN_LESION = "skin_lesion.tflite"
        const val MODEL_OCT_RETINA = "oct_retina.tflite"
        const val MODEL_BLOOD_CELL = "blood_cell.tflite"
        const val MODEL_COLON_PATH = "colon_path.tflite"
        // Expanded screening models (28x28 inputs, FP16 quantized)
        const val MODEL_CHEST_XRAY = "chest_xray.tflite"
        const val MODEL_KIDNEY_TISSUE = "tissue_kidney.tflite"
        const val MODEL_CT_ORGAN_AXIAL = "organ_axial.tflite"
        const val MODEL_CT_ORGAN_CORONAL = "organ_coronal.tflite"
        const val MODEL_CT_ORGAN_SAGITTAL = "organ_sagittal.tflite"
    }
}
