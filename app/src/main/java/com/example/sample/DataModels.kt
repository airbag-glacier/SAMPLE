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

data class CloudSyncPayload(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("user_profile") val userProfile: Map<String, String>?,
    @SerializedName("emergency_contacts") val emergencyContacts: List<Map<String, String>>,
    @SerializedName("appointments") val appointments: List<Map<String, String>>,
    @SerializedName("latest_facial_scan") val latestFacialScan: Map<String, Any>?,
    @SerializedName("latest_risk_assessment") val latestRiskAssessment: Map<String, Any>?
)

data class SyncResponse(
    val success: Boolean,
    val message: String?,
    val error: String?
)

// Add to DataModels.kt
data class LoginCredentials(
    val email: String,
    val passwordHash: String // Always hash passwords in production!
)

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    @com.google.gson.annotations.SerializedName("user_data")
    val userData: CloudSyncPayload?
)