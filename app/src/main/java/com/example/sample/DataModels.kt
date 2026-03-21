package com.example.sample

import com.google.gson.annotations.SerializedName


data class PatientDataPayload(
    val age: Int,
    val gender: String, // e.g., "Male" or "Female"
    val hypertension: Int, // 1 or 0

    @SerializedName("heart_disease")
    val heartDisease: Int, // Maps to SQLite cardiac_disease (1 or 0)

    val bmi: Double,

    @SerializedName("smoking_status")
    val smokingStatus: String // e.g., "smokes", "never smoked"
)

// This unpacks the
// {"success": True, "risk_score": 0.75}
// JSON response from app.py
data class PredictionResponse(
    val success: Boolean,

    @SerializedName("risk_score")
    val riskScore: Double?,

    val error: String?
)

// The master payload containing all local SQLite data for the user
data class CloudSyncPayload(
    @com.google.gson.annotations.SerializedName("user_id")
    val userId: Long,

    @com.google.gson.annotations.SerializedName("user_profile")
    val userProfile: Map<String, String>?,

    @com.google.gson.annotations.SerializedName("emergency_contacts")
    val emergencyContacts: List<Map<String, String>>,

    @com.google.gson.annotations.SerializedName("latest_facial_scan")
    val latestFacialScan: Map<String, Any>?
)

// The response from your Python server confirming the Google Cloud SQL write
data class SyncResponse(
    val success: Boolean,
    val message: String?,
    val error: String?
)