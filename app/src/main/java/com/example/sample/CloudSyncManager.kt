package com.example.sample

import com.google.gson.Gson
import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CloudSyncManager(private val context: Context) {

    // Ensure this matches your Python Flask server's Wi-Fi IP address
    private val BASE_URL = "http://192.168.254.121:5000/"

    fun syncLocalDatabaseToCloud(userId: Long) {
        val dbHelper = DatabaseHelper(context)
        Log.d("CloudSync", "Initiating background sync for User ID: $userId")

        // Extract records from local SQLite
        val profileData = dbHelper.getFullUserProfile(userId)
        val contactsData = dbHelper.getEmergencyContacts(userId)
        val latestScanData = dbHelper.getLatestFacialScan(userId)
        val appointmentsData = dbHelper.getAppointments(userId)

        //PAYLOAD ASSEMBLY
        val syncPayload = CloudSyncPayload(
            userId = userId,
            userProfile = profileData,
            emergencyContacts = contactsData,
            latestFacialScan = latestScanData,
            appointments = appointmentsData
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
}