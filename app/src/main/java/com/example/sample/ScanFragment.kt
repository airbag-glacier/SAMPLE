package com.example.sample

import android.Manifest
import android.content.pm.PackageManager
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
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class ScanFragment : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView

    // Handles the "Dangerous" Camera Permission Popup safely
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to scan.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewFinder = view.findViewById(R.id.viewFinder)

        // Setup Back Button
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        // Setup the Capture Button from your custom layout
        view.findViewById<Button>(R.id.btnCapture).setOnClickListener {
            takePhoto()
        }

        // Check Permissions when screen opens
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

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            // We default to the front-facing camera for facial scans
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

        // Create a temporary file to hold the captured image
        val photoFile = File(requireContext().cacheDir, "face_scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        Toast.makeText(requireContext(), "Analyzing face...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo capture failed.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Photo successfully saved! Now send it to the Python YOLO server
                    uploadImageToPython(photoFile)
                }
            }
        )
    }

    private fun uploadImageToPython(file: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // IMPORTANT: Ensure this IP perfectly matches your laptop's current Wi-Fi IP
                val url = URL("http://192.168.1.15:5000/detect_face")
                val connection = url.openConnection() as HttpURLConnection
                val boundary = "Boundary-${System.currentTimeMillis()}"

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    // 1. Write File Header
                    os.write("--$boundary\r\n".toByteArray())
                    os.write("Content-Disposition: form-data; name=\"image\"; filename=\"${file.name}\"\r\n".toByteArray())
                    os.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())

                    // 2. Write File Bytes
                    FileInputStream(file).use { fis ->
                        fis.copyTo(os)
                    }

                    // 3. Write Closing Boundary
                    os.write("\r\n--$boundary--\r\n".toByteArray())
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(response)

                    withContext(Dispatchers.Main) {
                        if (responseJson.getBoolean("success")) {
                            val detections = responseJson.getJSONArray("detections")
                            showResultDialog(detections.length())
                        } else {
                            Toast.makeText(requireContext(), "Server Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Connection failed.", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("YOLO", "Error: ${e.message}")
                    Toast.makeText(requireContext(), "Network error. Is server running?", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showResultDialog(detectionCount: Int) {
        val message = if (detectionCount > 0) {
            "Warning: $detectionCount facial drooping features detected! Please seek immediate medical attention."
        } else {
            "No facial drooping features detected."
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("YOLOv10 Assessment")
            .setMessage(message)
            .setPositiveButton("Done") { _, _ -> findNavController().popBackStack() }
            .setCancelable(false)
            .show()
    }
}