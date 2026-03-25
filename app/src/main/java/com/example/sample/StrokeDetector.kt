package com.example.sample

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

class StrokeDetector(context: Context) {
    private var interpreter: Interpreter? = null
    private val inputSize = 640

    init {
        try {
            val model = loadModelFile(context, "detech_stroke_model.tflite")
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        return inputStream.channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun detect(bitmap: Bitmap): ScanResult {
        val tInterpreter = interpreter ?: return ScanResult(0f, 0f, emptyList())

        // Native Resizing & Normalization
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }
        tInterpreter.run(byteBuffer, outputBuffer)

        val leftEyeY = mutableListOf<Float>()
        val rightEyeY = mutableListOf<Float>()
        val leftLipY = mutableListOf<Float>()
        val rightLipY = mutableListOf<Float>()
        val detections = outputBuffer[0]

        for (i in detections.indices) {
            val confidence = detections[i][4]
            val classId = detections[i][5].toInt()
            val yCenter = ((detections[i][1] + detections[i][3]) / 2f) / inputSize // Normalized Y

            if (confidence > 0.40f) {
                when (classId) {
                    1 -> leftEyeY.add(yCenter)
                    2 -> rightEyeY.add(yCenter)
                    3 -> leftLipY.add(yCenter)
                    4 -> rightLipY.add(yCenter)
                }
            }
        }

        var eyeAsymmetryFlag = 0.0f
        var mouthAsymmetryFlag = 0.0f
        val foundSymptoms = mutableListOf<String>()
        val droopThreshold = 0.035f

        if (leftEyeY.isNotEmpty() && rightEyeY.isNotEmpty()) {
            if (abs(leftEyeY[0] - rightEyeY[0]) > droopThreshold) {
                eyeAsymmetryFlag = 1.0f
                foundSymptoms.add("Noticeable Eye Asymmetry")
            }
        }

        if (leftLipY.isNotEmpty() && rightLipY.isNotEmpty()) {
            if (abs(leftLipY[0] - rightLipY[0]) > droopThreshold) {
                mouthAsymmetryFlag = 1.0f
                foundSymptoms.add("Noticeable Mouth Droop")
            }
        }

        return ScanResult(eyeAsymmetryFlag, mouthAsymmetryFlag, foundSymptoms)
    }
}

data class ScanResult(val eyeAsymmetry: Float, val mouthAsymmetry: Float, val symptoms: List<String>)