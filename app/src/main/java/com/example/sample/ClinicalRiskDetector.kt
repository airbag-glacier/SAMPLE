package com.example.sample

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class ClinicalRiskDetector(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val model: MappedByteBuffer = FileUtil.loadMappedFile(context, "clinical_risk_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            Log.e("ML_ERROR", "Failed to load Clinical Model: ${e.message}")
            e.printStackTrace()
        }
    }

    fun predictRisk(encodedFeatures: FloatArray): Float {
        // TRAP DOOR: If the model failed to load, return a negative number so the UI shows -100%
        val tInterpreter = interpreter ?: return -1.0f

        // Bypass the clunky ByteBuffer and let TFLite process the raw arrays directly
        val inputArray = arrayOf(encodedFeatures)
        val outputArray = Array(1) { FloatArray(1) }

        try {
            tInterpreter.run(inputArray, outputArray)
            return outputArray[0][0]
        } catch (e: Exception) {
            Log.e("ML_ERROR", "Inference crashed: ${e.message}")
            return -1.0f // Return -100% if the TFLite math crashes
        }
    }

    fun close() {
        interpreter?.close()
    }
}