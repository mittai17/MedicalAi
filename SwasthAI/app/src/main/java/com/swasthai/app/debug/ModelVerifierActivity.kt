package com.swasthai.app.debug

import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.swasthai.app.ai.engine.ImageClassifier
import com.swasthai.app.ai.engine.ScanType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug-only on-device verifier. Loads every bundled TFLite model, feeds a
 * synthetic input shaped to the model's *declared* input tensor, runs a forward
 * pass, and reports pass/fail. Also exercises the real [ImageClassifier] path.
 * Launch with:
 *   adb shell am start -n com.swasthai.app/.debug.ModelVerifierActivity
 */
@AndroidEntryPoint
class ModelVerifierActivity : ComponentActivity() {

    @Inject lateinit var imageClassifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val log = StringBuilder()
        val results = mutableListOf<String>()

        fun logline(s: String) {
            log.appendLine(s)
            Log.i("MODEL_VERIFY", s)
        }

        // 1) Raw TFLite forward-pass check for every model asset
        val assetsAssets = assets.list("") ?: emptyArray()
        val modelFiles = assetsAssets.filter { it.endsWith(".tflite") }.sorted()
        logline("=== RAW TFLITE FORWARD-PASS CHECK (${modelFiles.size} models) ===")
        for (name in modelFiles) {
            results.add(rawCheck(name, ::logline))
        }

        // 2) Real ImageClassifier.classify() path with a generated bitmap
        logline("")
        logline("=== REAL ImageClassifier.classify() CHECKS ===")
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF808080.toInt())
        val cacheFile = File(cacheDir, "verify_input.png")
        FileOutputStream(cacheFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = Uri.fromFile(cacheFile)
        for (st in ScanType.entries) {
            val r = try {
                imageClassifier.classify(uri.toString(), st)
            } catch (e: Throwable) {
                null
            }
            if (r == null) {
                logline("CLASSIFY ${st.name}: FAIL (returned null / threw)")
                results.add("ImageClassifier.${st.name}=FAIL")
            } else {
                logline("CLASSIFY ${st.name}: OK label=${r.label} conf=${"%.3f".format(r.confidence)} confident=${r.isConfident}")
                results.add("ImageClassifier.${st.name}=OK")
            }
        }
        imageClassifier.close()

        // ---- Render on screen ----
        val pad = (16 * resources.displayMetrics.density).toInt()
        val scroll = ScrollView(this)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(pad, pad, pad, pad)
        val tv = TextView(this)
        tv.text = log.toString()
        tv.textSize = 11f
        tv.setTypeface(android.graphics.Typeface.MONOSPACE)
        container.addView(tv)
        scroll.addView(container)
        setContentView(scroll)

        val failed = results.count { it.endsWith("=FAIL") }
        logline("")
        logline("SUMMARY: ${results.size - failed}/${results.size} checks OK, $failed FAILED")
    }

    private fun rawCheck(model: String, logline: (String) -> Unit): String {
        return try {
            val bytes = assets.open(model).use { it.readBytes() }
            val buf = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buf.put(bytes).rewind()
            val interp = Interpreter(buf)
            interp.allocateTensors()

            val inDesc = interp.getInputTensor(0)
            val inShape = inDesc.shape()
            val inSize = inShape.fold(1) { a, d -> a * d }
            val inElem = inDesc.dataType().byteSize()
            val input = ByteBuffer.allocateDirect(inSize * inElem).order(ByteOrder.nativeOrder())
            for (i in 0 until inSize * inElem) input.put(0)
            input.rewind()

            val outDesc = interp.getOutputTensor(0)
            val outShape = outDesc.shape()
            val outSize = outShape.fold(1) { a, d -> a * d }
            val outElem = outDesc.dataType().byteSize()
            val output = ByteBuffer.allocateDirect(outSize * outElem).order(ByteOrder.nativeOrder())
            output.rewind()

            interp.run(input, output)
            output.rewind()

            // finite check on floating output
            val finite = if (outDesc.dataType() == org.tensorflow.lite.DataType.FLOAT32) {
                var ok = true
                for (i in 0 until outSize) if (!output.float.isFinite()) { ok = false; break }
                ok
            } else {
                true
            }

            val inSig = "${inShape.contentToString()} ${inDesc.dataType()}"
            val outSig = "${outShape.contentToString()} ${outDesc.dataType()}"
            interp.close()
            val status = if (finite) "OK" else "FAIL(nonfinite)"
            logline("TFLITE $model -> $status  in=$inSig  out=$outSig")
            status
        } catch (e: Throwable) {
            logline("TFLITE $model -> FAIL ($e)")
            "FAIL"
        }
    }
}
