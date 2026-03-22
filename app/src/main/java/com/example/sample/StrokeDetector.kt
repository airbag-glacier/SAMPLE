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

        // 1. Pre-process (Standard 640x640 for YOLOv10)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.Method.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        var tensorImage = TensorImage(tInterpreter.getInputTensor(0).dataType())
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Prepare Output [1, 300, 6]
        val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }

        // 3. Run AI
        tInterpreter.run(tensorImage.buffer, outputBuffer)

        // 4. Map Class IDs to Symptom Names
        // Update these strings to match the classes you used in Roboflow/Kaggle!
        val labels = mapOf(
            0 to "Mouth Droop",
            1 to "Eye Asymmetry",
            2 to "Eyebrow Displacement"
        )

        val foundSymptoms = mutableListOf<String>()
        val detections = outputBuffer[0]

        for (i in detections.indices) {
            val confidence = detections[i][4]
            val classId = detections[i][5].toInt()

            if (confidence > 0.30f) { // 45% threshold is usually good for mobile
                labels[classId]?.let { symptom ->
                    if (!foundSymptoms.contains(symptom)) {
                        foundSymptoms.add(symptom)
                    }
                }
            }
        }

        return foundSymptoms
    }
}