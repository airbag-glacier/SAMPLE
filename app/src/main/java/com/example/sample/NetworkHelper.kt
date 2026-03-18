package com.example.sample

import android.content.Context
import android.util.Log
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. DATA MODELS (JSON MAPPING)
// ==========================================

// Maps the Kotlin data into the JSON format expected by your Python Pandas DataFrame
data class PatientDataPayload(
    val age: Int,
    val gender: String,
    val hypertension: Int,
    @SerializedName("heart_disease")
    val heartDisease: Int,
    val bmi: Double,
    @SerializedName("smoking_status")
    val smokingStatus: String
)

// Maps the {"success": True, "risk_score": 0.75} JSON response from app.py
data class PredictionResponse(
    val success: Boolean,
    @SerializedName("risk_score")
    val riskScore: Double?,
    val error: String?
)

// ==========================================
// 2. RETROFIT API INTERFACE
// ==========================================

interface StrokePredictionApi {
    // This targets the @app.route('/predict_risk') endpoint in your Flask app
    @POST("predict_risk")
    fun getRiskPrediction(@Body patientData: PatientDataPayload): Call<PredictionResponse>
}

// ==========================================
// 3. MAIN NETWORK HELPER CLASS
// ==========================================

class NetworkHelper(private val context: Context) {

    // IMPORTANT FOR DEMO: Change this to your laptop's actual Wi-Fi IPv4 address!
    private val BASE_URL = "http://192.168.1.15:5000/"

    fun fetchAndSendUserData(userId: Long) {
        val dbHelper = DatabaseHelper(context)

        // Extract the local SQLite data using your existing helper method
        val userProfile = dbHelper.getFullUserProfile(userId)

        // Parse strings back into the numerical formats required by Logistic Regression
        val payload = PatientDataPayload(
            age = userProfile["age"]?.toIntOrNull() ?: 0,
            gender = userProfile["sex"] ?: "Unknown",
            hypertension = if (userProfile["hypertension"] == "Yes") 1 else 0,
            heartDisease = 0, // Defaulting to 0; add to getFullUserProfile() in DatabaseHelper if needed
            bmi = userProfile["bmi"]?.toDoubleOrNull() ?: 0.0,
            smokingStatus = if (userProfile["smoker"] == "Yes") "smokes" else "never smoked"
        )

        Log.d("NetworkHelper", "Sending Payload: $payload")

        // Build the Retrofit Client
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(StrokePredictionApi::class.java)

        // Send the Data to the Flask Server Asynchronously
        api.getRiskPrediction(payload).enqueue(object : Callback<PredictionResponse> {

            override fun onResponse(call: Call<PredictionResponse>, response: Response<PredictionResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {

                    val riskScore = response.body()?.riskScore ?: 0.0
                    Log.d("ServerResponse", "Success! Stroke Risk is: ${riskScore * 100}%")

                    // Categorize the risk level based on the mathematical probability
                    val riskLevel = when {
                        riskScore >= 0.70 -> "High"
                        riskScore >= 0.30 -> "Moderate"
                        else -> "Low"
                    }

                    // Generate a standard timestamp
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentTimestamp = sdf.format(Date())

                    // Save the calculation directly back into the local Android database
                    val isSaved = dbHelper.insertRiskAssessment(
                        userId = userId,
                        lrPrediction = riskScore,
                        riskLevel = riskLevel,
                        timestamp = currentTimestamp
                    )

                    if (isSaved) {
                        Log.d("Database", "Assessment saved successfully: $riskLevel Risk ($riskScore)")
                        // Trigger a UI update broadcast here if your Dashboard is currently open
                    } else {
                        Log.e("Database", "Failed to save the risk assessment to SQLite.")
                    }

                } else {
                    Log.e("ServerResponse", "Server error: ${response.body()?.error}")
                }
            }

            override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                Log.e("ServerResponse", "Network Request Failed: ${t.message}")
            }
        })
    }
}