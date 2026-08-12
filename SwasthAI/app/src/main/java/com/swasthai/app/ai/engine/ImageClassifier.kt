package com.swasthai.app.ai.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of classifying a medical image with a [ScanType] model.
 */
data class ClassificationResult(
    val scanType: ScanType,
    val label: String,
    val confidence: Float,
    val isConfident: Boolean,
    val findings: List<String> = emptyList()
)

/**
 * Runs on-device screening models (all 28x28 input, normalized to [0, 1]).
 *
 * Binary models emit a single sigmoid (positive-class probability, index 0
 * in [ScanType.labels]); multiclass models emit a softmax over the labels.
 * A result is reported only when the model output clears [ScanType.reportGate].
 */
@Singleton
class ImageClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val interpreters = ConcurrentHashMap<ScanType, Interpreter>()

    private fun getInterpreter(scanType: ScanType): Interpreter? {
        return interpreters.getOrPut(scanType) {
            try {
                val bytes = context.assets.open(scanType.modelFile).use { it.readBytes() }
                val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
                buffer.put(bytes).rewind()
                Interpreter(buffer).also {
                    it.allocateTensors()
                }
            } catch (_: IOException) {
                null
            }
        }
    }

    /**
     * Classify the image at [imagePath] using the model for [scanType].
     *
     * @return classification result, or null if the model/asset is unavailable
     *         or the image cannot be decoded.
     */
    fun classify(imagePath: String, scanType: ScanType): ClassificationResult? {
        val interpreter = getInterpreter(scanType) ?: return null
        val bitmap = decodeBitmap(imagePath) ?: return null

        val input = preprocess(bitmap, scanType)
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputSize = outputShape.fold(1) { acc, d -> acc * d }
        val output = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder())

        interpreter.run(input, output)
        output.rewind()

        val probs = FloatArray(outputSize) { output.float }
        return if (scanType.isMultilabel) {
            // Per-label sigmoids gated by calibrated per-label thresholds.
            val thresholds = scanType.perLabelThresholds
            val findings = mutableListOf<Pair<String, Float>>()
            for (i in probs.indices) {
                val thr = thresholds?.getOrNull(i) ?: 0.5f
                if (probs[i] >= thr && i < scanType.labels.size) {
                    findings.add(scanType.labels[i] to probs[i])
                }
            }
            findings.sortByDescending { it.second }
            if (findings.isEmpty()) {
                // Nothing crossed a threshold: report the closest label, not confident.
                var bestIdx = 0
                for (i in probs.indices) {
                    if (probs[i] > probs[bestIdx]) bestIdx = i
                }
                ClassificationResult(
                    scanType = scanType,
                    label = scanType.labels[bestIdx],
                    confidence = probs[bestIdx],
                    isConfident = false,
                    findings = emptyList()
                )
            } else {
                val primary = findings.first()
                ClassificationResult(
                    scanType = scanType,
                    label = primary.first,
                    confidence = primary.second,
                    isConfident = primary.second >= scanType.reportGate,
                    findings = findings.map { it.first }
                )
            }
        } else if (scanType.isBinary) {
            // Single sigmoid = probability of labels[0] (positive class).
            val positiveProb = probs[0]
            val confident = positiveProb >= scanType.reportGate
            ClassificationResult(
                scanType = scanType,
                label = if (confident) scanType.labels[0] else scanType.labels[1],
                confidence = positiveProb,
                isConfident = confident
            )
        } else {
            var bestIdx = 0
            for (i in probs.indices) {
                if (probs[i] > probs[bestIdx]) bestIdx = i
            }
            ClassificationResult(
                scanType = scanType,
                label = scanType.labels[bestIdx],
                confidence = probs[bestIdx],
                isConfident = probs[bestIdx] >= scanType.reportGate
            )
        }
    }

    /**
     * Decode the image, resize to 28x28, normalize to [0, 1].
     * Single-channel models take grayscale luminance; three-channel take RGB.
     */
    private fun preprocess(bitmap: Bitmap, scanType: ScanType): ByteBuffer {
        val size = scanType.inputSize
        val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val channels = scanType.inputChannels
        val buffer = ByteBuffer.allocateDirect(size * size * channels * 4).order(ByteOrder.nativeOrder())

        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = resized.getPixel(x, y)
                val r = (pixel shr 16 and 0xFF) / 255f
                val g = (pixel shr 8 and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                if (channels == 1) {
                    buffer.putFloat(r * 0.299f + g * 0.587f + b * 0.114f)
                } else {
                    buffer.putFloat(r)
                    buffer.putFloat(g)
                    buffer.putFloat(b)
                }
            }
        }
        resized.recycle()
        buffer.rewind()
        return buffer
    }

    private fun decodeBitmap(imagePath: String): Bitmap? {
        return try {
            val uri = Uri.parse(imagePath)
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        interpreters.values.forEach { it.close() }
        interpreters.clear()
    }
}
