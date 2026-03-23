package com.example.sample

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.MappedByteBuffer
import kotlin.math.abs

class StrokeDetector(context: Context) {
    private var interpreter: Interpreter? = null
    private val inputSize = 640

    init {
        try {
            val model: MappedByteBuffer = FileUtil.loadMappedFile(context, "detech_stroke_model.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun detect(bitmap: Bitmap): List<String> {
        val tInterpreter = interpreter ?: return emptyList()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        var tensorImage = TensorImage(tInterpreter.getInputTensor(0).dataType())
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }

        tInterpreter.run(tensorImage.buffer, outputBuffer)

        // Lists to store Y-coordinates for comparison
        val leftEyeY = mutableListOf<Float>()
        val rightEyeY = mutableListOf<Float>()
        val leftLipY = mutableListOf<Float>()
        val rightLipY = mutableListOf<Float>()

        val detections = outputBuffer[0]

        for (i in detections.indices) {
            val confidence = detections[i][4]
            val classId = detections[i][5].toInt()

            // Calculate center Y coordinate: (Ymin + Ymax) / 2
            val yCenter = (detections[i][1] + detections[i][3]) / 2f

            if (confidence > 0.40f) {
                when (classId) {
                    1 -> leftEyeY.add(yCenter)
                    2 -> rightEyeY.add(yCenter)
                    3 -> leftLipY.add(yCenter)
                    4 -> rightLipY.add(yCenter)
                }
            }
        }

        // 5. Calculate Asymmetry
        val foundSymptoms = mutableListOf<String>()
        val droopThreshold = 0.035f // Adjusted for noticeable droop (approx 3.5% of image height)

        // Eye Comparison
        if (leftEyeY.isNotEmpty() && rightEyeY.isNotEmpty()) {
            val eyeDiff = abs(leftEyeY[0] - rightEyeY[0])
            Log.d("StrokeDetector", "Eye Y-Diff: $eyeDiff")
            if (eyeDiff > droopThreshold) {
                foundSymptoms.add("Noticeable Eye Asymmetry")
            }
        }

        // Lip Corner Comparison
        if (leftLipY.isNotEmpty() && rightLipY.isNotEmpty()) {
            val lipDiff = abs(leftLipY[0] - rightLipY[0])
            Log.d("StrokeDetector", "Lip Y-Diff: $lipDiff")
            if (lipDiff > droopThreshold) {
                foundSymptoms.add("Noticeable Mouth Droop")
            }
        }

        return foundSymptoms
    }
}