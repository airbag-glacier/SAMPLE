package com.example.sample

import android.content.Context
import android.graphics.Bitmap
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

    // FIX 1: Strict return type
    fun detect(bitmap: Bitmap): ScanResult {
        val tInterpreter = interpreter ?: return ScanResult(0f, 0f, emptyList())

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        var tensorImage = TensorImage(tInterpreter.getInputTensor(0).dataType())
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }
        tInterpreter.run(tensorImage.buffer, outputBuffer)

        // FIX 3: Store pairs of (Y-Coordinate, Confidence) to find the best box
        var bestLeftEye: Pair<Float, Float>? = null
        var bestRightEye: Pair<Float, Float>? = null
        var bestLeftLip: Pair<Float, Float>? = null
        var bestRightLip: Pair<Float, Float>? = null

        val detections = outputBuffer[0]

        for (i in detections.indices) {
            val confidence = detections[i][4]
            val classId = detections[i][5].toInt()

            if (confidence > 0.40f) {
                // FIX 2: Normalize the Y-coordinate to a 0.0 - 1.0 scale
                val yCenter = ((detections[i][1] + detections[i][3]) / 2f) / inputSize

                // Update if this is the highest confidence detection we've seen for this class
                when (classId) {
                    1 -> if (bestLeftEye == null || confidence > bestLeftEye.second) bestLeftEye = Pair(yCenter, confidence)
                    2 -> if (bestRightEye == null || confidence > bestRightEye.second) bestRightEye = Pair(yCenter, confidence)
                    3 -> if (bestLeftLip == null || confidence > bestLeftLip.second) bestLeftLip = Pair(yCenter, confidence)
                    4 -> if (bestRightLip == null || confidence > bestRightLip.second) bestRightLip = Pair(yCenter, confidence)
                }
            }
        }

        // Calculate Asymmetry
        var eyeAsymmetryFlag = 0.0f
        var mouthAsymmetryFlag = 0.0f
        val foundSymptoms = mutableListOf<String>()

        val droopThreshold = 0.035f // 3.5% of image height

        // Eye Comparison
        if (bestLeftEye != null && bestRightEye != null) {
            val eyeDiff = abs(bestLeftEye.first - bestRightEye.first)
            if (eyeDiff > droopThreshold) {
                eyeAsymmetryFlag = 1.0f
                foundSymptoms.add("Noticeable Eye Asymmetry")
            }
        }

        // Lip Corner Comparison
        if (bestLeftLip != null && bestRightLip != null) {
            val lipDiff = abs(bestLeftLip.first - bestRightLip.first)
            if (lipDiff > droopThreshold) {
                mouthAsymmetryFlag = 1.0f
                foundSymptoms.add("Noticeable Mouth Droop")
            }
        }

        return ScanResult(eyeAsymmetryFlag, mouthAsymmetryFlag, foundSymptoms)
    }
}

data class ScanResult(val eyeAsymmetry: Float, val mouthAsymmetry: Float, val symptoms: List<String>)