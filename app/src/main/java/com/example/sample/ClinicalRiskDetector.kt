package com.example.sample

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

class ClinicalRiskDetector(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            // Load the converted TFLite model from the assets folder
            val model: MappedByteBuffer = FileUtil.loadMappedFile(context, "clinical_risk_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2) // 2 threads are plenty for a lightweight clinical model
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Predicts the stroke risk probability.
     * @param encodedFeatures A FloatArray containing the perfectly formatted data.
     * @return A Float between 0.0 and 1.0 representing the risk percentage.
     */
    fun predictRisk(encodedFeatures: FloatArray): Float {
        val tInterpreter = interpreter ?: return 0f

        // Prepare Input Buffer
        val inputBuffer = ByteBuffer.allocateDirect(4 * encodedFeatures.size).apply {
            order(ByteOrder.nativeOrder())
        }

        // Load the array data into the memory buffer
        for (value in encodedFeatures) {
            inputBuffer.putFloat(value)
        }
        //rewinds the memory cursor to the beginning of the buffer
        inputBuffer.rewind()

        // Prepare Output Buffer
        val outputBuffer = Array(1) { FloatArray(1) }

        // Run the algo Inference
        tInterpreter.run(inputBuffer, outputBuffer)

        // Return the predicted probability (e.g., 0.75 for 75% risk)
        return outputBuffer[0][0]
    }

    // Always close the interpreter when the app shuts down to prevent memory leaks
    fun close() {
        interpreter?.close()
    }
}