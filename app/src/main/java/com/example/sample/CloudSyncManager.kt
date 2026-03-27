package com.example.sample

import com.google.gson.Gson
import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File


class CloudSyncManager(private val context: Context) {

    // Ensure this matches your Python Flask server's Wi-Fi IP address
    private val BASE_URL = "http://192.168.0.199:5000/"

    fun syncLocalDatabaseToCloud(userId: Long) {
        val dbHelper = DatabaseHelper(context)
        Log.d("CloudSync", "Initiating background sync for User ID: $userId")

        // Extract records from local SQLite
        val profileData = dbHelper.getFullUserProfile(userId)
        val contactsData = dbHelper.getEmergencyContacts(userId)
        var latestScanData = dbHelper.getLatestFacialScan(userId)
        val appointmentsData = dbHelper.getAppointments(userId)
        val latestRiskData = dbHelper.getLatestRiskAssessment(userId)

        val imagePath = latestScanData?.get("image_path") as? String ?: ""
        val base64Image = encodeImageToBase64(imagePath)


        if (latestScanData != null && base64Image != null) {
            val mutableScanData = latestScanData.toMutableMap()
            mutableScanData["image_base64"] = base64Image
            latestScanData = mutableScanData
        }

        val syncPayload = CloudSyncPayload(
            userName = profileData["name"] ?: "Unknown User", // Use brackets for Maps
            userEmail = profileData["email"] ?: "N/A",        // Use brackets for Maps
            userId = userId,
            userProfile = profileData,
            emergencyContacts = contactsData,
            appointments = appointmentsData,
            latestFacialScan = latestScanData,
            latestRiskAssessment = latestRiskData
        )

        //LOGGING OF PAYLOAD: Verifying if the data is being sent
        val gson = Gson()
        val jsonPayload = gson.toJson(syncPayload)
        Log.d("CloudSync", "DATA BEING SENT TO CLOUD: $jsonPayload")

        // Retrofit Client
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // Use the variable that ends in a slash
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CloudSyncApi::class.java)

        // Transmit Payload
        api.pushDataToCloud(syncPayload).enqueue(object : Callback<SyncResponse> {
            override fun onResponse(call: Call<SyncResponse>, response: Response<SyncResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("CloudSync", "Successfully synced local data to Cloud.")
                } else {
                    Log.e("CloudSync", "Server rejected sync: ${response.body()?.error}")
                }
            }

            override fun onFailure(call: Call<SyncResponse>, t: Throwable) {
                Log.e("CloudSync", "Network request failed. Error: ${t.message}")
            }
        })
    }



    fun encodeImageToBase64(imagePath: String): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

            // 1. Load the original image
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)

            // 2. Resize it so it doesn't crash the database! (Max 800px)
            val maxWidth = 800
            val maxHeight = 800
            val scale = Math.min(
                maxWidth.toFloat() / originalBitmap.width,
                maxHeight.toFloat() / originalBitmap.height
            )
            val scaledWidth = (scale * originalBitmap.width).toInt()
            val scaledHeight = (scale * originalBitmap.height).toInt()
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

            // 3. Compress and Encode the smaller image
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}