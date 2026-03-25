package com.example.sample

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ClinicalRiskDetector(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val model = loadModelFile(context, "clinical_risk_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            Log.e("ML_ERROR", "Failed to load Clinical Model: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        return inputStream.channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun predictRisk(encodedFeatures: FloatArray): Float {
        val tInterpreter = interpreter ?: return -1.0f
        val inputArray = arrayOf(encodedFeatures)
        val outputArray = Array(1) { FloatArray(1) }

        try {
            tInterpreter.run(inputArray, outputArray)
            return outputArray[0][0]
        } catch (e: Exception) {
            Log.e("ML_ERROR", "Inference crashed: ${e.message}")
            return -1.0f
        }
    }

    fun close() {
        interpreter?.close()
    }
}