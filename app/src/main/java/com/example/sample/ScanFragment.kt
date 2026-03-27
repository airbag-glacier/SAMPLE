package com.example.sample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.graphics.Bitmap
import java.io.FileOutputStream

class ScanFragment : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView
    private lateinit var strokeDetector: StrokeDetector

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewFinder = view.findViewById(R.id.viewFinder)

        // Initialize the On-Device AI Model
        strokeDetector = StrokeDetector(requireContext())

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<Button>(R.id.btnCapture).setOnClickListener { takePhoto() }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Failed to open camera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(requireContext().cacheDir, "face_scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        Toast.makeText(requireContext(), "Running Offline AI Analysis...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo capture failed.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Photo saved. Run TFLite Inference!
                    runOfflineInference(photoFile)
                }
            }
        )
    }

    private fun runOfflineInference(file: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert the saved image file into a raw Bitmap
                val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (rawBitmap == null) {
                    throw Exception("Camera failed to process the image.")
                }

                // --- NEW CODE: Resize the giant photo to fit YOLO's strict input size ---
                // NOTE: Change 640 to 224 or 320 if your specific TFLite model requires a smaller square!
                val resizedBitmap = Bitmap.createScaledBitmap(rawBitmap, 640, 640, true)

                // 1. EXECUTE LOCAL TFLITE MODEL (Hand it the tiny resized one!)
                val scanResult = strokeDetector.detect(resizedBitmap)
                val symptomList = scanResult.symptoms

                // Save the original high-quality image to internal storage for the Cloud/Doctor
                val savedFilePath = saveYoloImageToInternalStorage(requireContext(), rawBitmap)

                // 2. SAVE TO SQLITE DATABASE
                saveScanToDatabase(symptomList.isNotEmpty(), savedFilePath)

                withContext(Dispatchers.Main) {
                    // Clean up the temp camera image
                    if (file.exists()) file.delete()

                    // Show Emergency Dialog
                    showResultDialog(symptomList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // THIS WILL PRINT THE ENTIRE RED CRASH LOG:
                    Log.e("TFLite", "Inference Error", e)
                    Toast.makeText(requireContext(), "AI Analysis failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Notice we added 'imagePath: String' to the parentheses here:
    private fun saveScanToDatabase(isAsymmetric: Boolean, imagePath: String) {
        val dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        if (userId != -1L) {
            val db = dbHelper.writableDatabase
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val values = android.content.ContentValues().apply {
                put("user_id", userId)
                put("asymmetric_detected", if (isAsymmetric) 1 else 0)
                put("confidence", 0.85)
                put("timestamp", timestamp)

                // --- NEW CODE: Write the path into the database! ---
                put("image_path", imagePath)
            }

            db.insert("FacialScanResult", null, values)
            db.close()

            // 3. TRIGGER CLOUD SYNC SO THE DOCTOR GETS THE RESULT!
            CloudSyncManager(requireContext()).syncLocalDatabaseToCloud(userId)
        }
    }

    private fun showResultDialog(symptoms: List<String>) {
        if (symptoms.isNotEmpty()) {
            val symptomsList = symptoms.joinToString(", ")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ CRITICAL: AI Assessment")
                .setMessage("Warning: Facial asymmetry detected ($symptomsList). This is a potential sign of a stroke. Please seek immediate medical attention.")
                .setPositiveButton("Find Hospital") { _, _ ->
                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=hospitals"))
                    mapIntent.setPackage("com.google.android.apps.maps")
                    try { startActivity(mapIntent) } catch (e: Exception) { Toast.makeText(requireContext(), "Maps not installed.", Toast.LENGTH_SHORT).show() }
                    findNavController().popBackStack()
                }
                .setNegativeButton("Call Emergency") { _, _ ->
                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:911") }
                    startActivity(dialIntent)
                    findNavController().popBackStack()
                }
                .setNeutralButton("Dismiss") { _, _ -> findNavController().popBackStack() }
                .setCancelable(false)
                .show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("AI Assessment")
                .setMessage("No facial drooping features detected. Your face appears symmetrical.")
                .setPositiveButton("Done") { _, _ -> findNavController().popBackStack() }
                .setCancelable(false)
                .show()
        }
    }

    fun saveYoloImageToInternalStorage(context: Context, scanBitmap: Bitmap): String {
        // 1. Create a unique filename
        val filename = "yolo_scan_${System.currentTimeMillis()}.jpg"

        // 2. Point to the app's secure internal storage directory (No permissions needed!)
        val file = File(context.filesDir, filename)

        // 3. Save the image
        FileOutputStream(file).use { outStream ->
            scanBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outStream)
        }

        // 4. Return this exact path to save into your local SQLite Database!
        return file.absolutePath
    }
}