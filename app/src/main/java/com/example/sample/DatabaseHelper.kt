package com.example.sample

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        // --- 7. APPOINTMENTS TABLE ---
        private const val TABLE_APPOINTMENTS = "Appointments"
        private const val COL_APT_ID = "apt_id"
        private const val COL_DOCTOR_NAME = "doctor_name"
        private const val COL_APT_DATE = "apt_date"
        private const val COL_APT_TIME = "apt_time"
        private const val DATABASE_NAME = "DeTechStroke.db"

        private const val DATABASE_VERSION = 5

        // --- 1. USER TABLE (Hybrid: ERD + Auth) ---
        private const val TABLE_USER = "User"
        private const val COL_USER_ID = "user_id"
        private const val COL_USER_NAME = "user_name"
        private const val COL_EMAIL = "email"
        private const val COL_PASSWORD = "password"
        private const val COL_IMAGE_URI = "image_uri"
        private const val COL_AGE = "age"
        private const val COL_SEX = "sex"

        // --- 2. HEALTH RISK FACTOR PROFILE TABLE ---
        private const val TABLE_HEALTH_PROFILE = "HealthRiskFactorProfile"
        private const val COL_PROFILE_ID = "profile_id"
        private const val COL_HYPERTENSION = "hypertension"
        private const val COL_DIABETES = "diabetes"
        private const val COL_SMOKER = "smoker"

        private const val COL_STROKE_HISTORY = "stroke_history"
        private const val COL_CARDIAC_DISEASE = "cardiac_disease"
        private const val COL_OBESE = "obese"
        private const val COL_UNHEALTHY_DIET = "unhealthy_diet"
        private const val COL_PHYSICAL_INABILITY = "physical_inability"
        private const val COL_ALCOHOLIC = "alcoholic"
        private const val COL_BMI = "bmi"

        // --- 3. EMERGENCY CONTACTS TABLE ---
        private const val TABLE_EMERGENCY_CONTACTS = "EmergencyContacts"
        private const val COL_CONTACT_ID = "contact_id"
        private const val COL_CONTACT_NAME = "name"
        private const val COL_RELATIONSHIP = "relationship"
        private const val COL_IS_PRIMARY = "is_primary"
        private const val COL_PHONE_NUMBER = "phone_number"

        // --- 4. FACIAL SCAN SCHEDULE TABLE ---
        private const val TABLE_SCAN_SCHEDULE = "FacialScanSchedule"
        private const val COL_SCHED_ID = "sched_id"
        private const val COL_SCHEDULE = "schedule"
        private const val COL_COMPLETED = "completed"

        // --- 5. FACIAL SCAN RESULT TABLE ---
        private const val TABLE_SCAN_RESULT = "FacialScanResult"
        private const val COL_SCAN_ID = "scan_id"
        private const val COL_ASYMMETRIC_DETECTED = "asymmetric_detected"
        private const val COL_CONFIDENCE = "confidence"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_IMAGE_PATH = "image_path"

        // --- 6. RISK ASSESSMENT RESULT TABLE ---
        private const val TABLE_RISK_ASSESSMENT = "risk_assessments"
        private const val COL_RISK_ID = "risk_id"
        private const val COL_LR_PREDICTION = "lr_prediction"
        private const val COL_RISK_LEVEL = "risk_level"


        private const val COL_CHOLESTEROL = "cholesterol_level"
        private const val COL_HDL = "hdl_level"               // ADD THIS
        private const val COL_LDL = "ldl_level"               // ADD THIS
        private const val COL_TRIGLYCERIDES = "triglycerides" // ADD THIS
        private const val COL_FBS = "fasting_blood_sugar"     // ADD THIS
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {

        val createAppointmentsTable = ("CREATE TABLE $TABLE_APPOINTMENTS ("
                + "$COL_APT_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER,"
                + "$COL_DOCTOR_NAME TEXT,"
                + "$COL_APT_DATE TEXT,"
                + "$COL_APT_TIME TEXT,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createAppointmentsTable)

        val createUserTable = ("CREATE TABLE $TABLE_USER ("
                + "$COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_NAME TEXT,"
                + "$COL_EMAIL TEXT UNIQUE,"
                + "$COL_PASSWORD TEXT,"
                + "$COL_IMAGE_URI TEXT,"
                + "$COL_AGE INTEGER,"
                + "$COL_SEX TEXT)")
        db.execSQL(createUserTable)

        val createHealthProfileTable = ("CREATE TABLE $TABLE_HEALTH_PROFILE ("
                + "$COL_PROFILE_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER UNIQUE,"
                + "$COL_HYPERTENSION INTEGER,"
                + "$COL_DIABETES INTEGER,"
                + "$COL_SMOKER INTEGER,"
                + "$COL_CHOLESTEROL REAL,"
                + "$COL_STROKE_HISTORY INTEGER,"
                + "$COL_CARDIAC_DISEASE INTEGER,"
                + "$COL_OBESE INTEGER,"
                + "$COL_UNHEALTHY_DIET INTEGER,"
                + "$COL_PHYSICAL_INABILITY INTEGER,"
                + "$COL_ALCOHOLIC INTEGER,"
                + "$COL_BMI REAL,"
                + "$COL_HDL REAL,"                 // <-- ADD THIS
                + "$COL_LDL REAL,"                 // <-- ADD THIS
                + "$COL_TRIGLYCERIDES REAL,"       // <-- ADD THIS
                + "$COL_FBS REAL,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createHealthProfileTable)

        // UPDATED: Added name, relationship, and is_primary
        val createEmergencyContactsTable = ("CREATE TABLE $TABLE_EMERGENCY_CONTACTS ("
                + "$COL_CONTACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER,"
                + "$COL_CONTACT_NAME TEXT,"
                + "$COL_RELATIONSHIP TEXT,"
                + "$COL_IS_PRIMARY INTEGER DEFAULT 0,"
                + "$COL_PHONE_NUMBER TEXT,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createEmergencyContactsTable)

        val createScanScheduleTable = ("CREATE TABLE $TABLE_SCAN_SCHEDULE ("
                + "$COL_SCHED_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER,"
                + "$COL_SCHEDULE TEXT,"
                + "$COL_COMPLETED INTEGER DEFAULT 0,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createScanScheduleTable)

        val createScanResultTable = ("CREATE TABLE $TABLE_SCAN_RESULT ("
                + "$COL_SCAN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER,"
                + "$COL_ASYMMETRIC_DETECTED INTEGER,"
                + "$COL_CONFIDENCE REAL,"
                + "$COL_TIMESTAMP TEXT,"
                + "$COL_IMAGE_PATH TEXT,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createScanResultTable)

        val createRiskAssessmentTable = ("CREATE TABLE $TABLE_RISK_ASSESSMENT ("
                + "$COL_RISK_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER,"
                + "$COL_LR_PREDICTION REAL,"
                + "$COL_RISK_LEVEL TEXT,"
                + "$COL_TIMESTAMP TEXT,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createRiskAssessmentTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RISK_ASSESSMENT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCAN_RESULT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCAN_SCHEDULE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMERGENCY_CONTACTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HEALTH_PROFILE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APPOINTMENTS")
        onCreate(db)
    }

    // ==========================================
    // AUTHENTICATION & UI HELPERS
    // ==========================================

    fun registerUser(email: String, password: String, name: String, imageUri: String): Boolean {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USER WHERE $COL_EMAIL = ?", arrayOf(email))
        if (cursor.count > 0) {
            cursor.close()
            return false
        }
        cursor.close()

        val values = ContentValues().apply {
            put(COL_USER_NAME, name)
            put(COL_EMAIL, email)
            put(COL_PASSWORD, password)
            put(COL_IMAGE_URI, imageUri)
        }
        val result = db.insert(TABLE_USER, null, values)
        db.close()
        return result != -1L
    }

    fun authenticateUser(email: String, password: String): Long {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_USER_ID FROM $TABLE_USER WHERE $COL_EMAIL = ? AND $COL_PASSWORD = ?", arrayOf(email, password))
        var userId = -1L
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(0)
        }
        cursor.close()
        return userId
    }

    fun getUserData(userId: Long): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_USER_NAME, $COL_IMAGE_URI FROM $TABLE_USER WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))
        var userData: Map<String, String>? = null
        if (cursor.moveToFirst()) {
            userData = mapOf(
                "name" to (cursor.getString(0) ?: "User"),
                "image_uri" to (cursor.getString(1) ?: "")
            )
        }
        cursor.close()
        return userData
    }

    fun getUserHealthSummary(userId: Long): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_HEALTH_PROFILE WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))
        val summary = if (cursor.moveToFirst()) {
            "Health Profile Configured. Tap to view."
        } else {
            "No health data available. Please complete your checkup."
        }
        cursor.close()
        return summary
    }

    // ==========================================
    // ERD DATA MAPPING FUNCTIONS
    // ==========================================

    private fun ensureProfileExists(userId: Long) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $COL_PROFILE_ID FROM $TABLE_HEALTH_PROFILE WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))
        if (!cursor.moveToFirst()) {
            val values = ContentValues().apply { put(COL_USER_ID, userId) }
            db.insert(TABLE_HEALTH_PROFILE, null, values)
        }
        cursor.close()
    }

    fun updateVitalsToERD(userId: Long, bmi: Double): Boolean {
        ensureProfileExists(userId)
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COL_BMI, bmi) }
        val rows = db.update(TABLE_HEALTH_PROFILE, values, "$COL_USER_ID = ?", arrayOf(userId.toString()))
        db.close()
        return rows > 0
    }

    fun updateBloodChemToERD(userId: Long, totalChol: Double, hdl: Double, ldl: Double, tri: Double, fbs: Double): Boolean {
        ensureProfileExists(userId)
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_CHOLESTEROL, totalChol)
            put(COL_HDL, hdl)
            put(COL_LDL, ldl)
            put(COL_TRIGLYCERIDES, tri)
            put(COL_FBS, fbs)
        }
        val rows = db.update(TABLE_HEALTH_PROFILE, values, "$COL_USER_ID = ?", arrayOf(userId.toString()))
        db.close()
        return rows > 0
    }

    fun updateRiskFactorsToERD(userId: Long, age: Int, hypertension: Int, cardiacDisease: Int, bmi: Double, smoker: Int, diabetes: Int): Boolean {
        ensureProfileExists(userId)
        val db = this.writableDatabase
        val userValues = ContentValues().apply { put(COL_AGE, age) }
        db.update(TABLE_USER, userValues, "$COL_USER_ID = ?", arrayOf(userId.toString()))

        val profileValues = ContentValues().apply {
            put(COL_HYPERTENSION, hypertension)
            put(COL_CARDIAC_DISEASE, cardiacDisease)
            put(COL_BMI, bmi)
            put(COL_SMOKER, smoker)
            put(COL_DIABETES, diabetes)
        }
        val rows = db.update(TABLE_HEALTH_PROFILE, profileValues, "$COL_USER_ID = ?", arrayOf(userId.toString()))
        db.close()
        return rows > 0
    }

    fun getFullUserProfile(userId: Long): Map<String, String> {
        val db = this.readableDatabase
        val map = mutableMapOf<String, String>()

        // Added diabetes, stroke_history, and cardiac_disease
        val keys = listOf("name", "email", "age", "sex", "bmi", "cholesterol", "hdl", "ldl", "tri", "fbs", "hypertension", "smoker", "diabetes", "stroke_history", "cardiac_disease", "image_uri")
        keys.forEach { map[it] = "N/A" }
        map["image_uri"] = ""

        val query = """
        SELECT u.$COL_USER_NAME, u.$COL_EMAIL, u.$COL_AGE, u.$COL_SEX, u.$COL_IMAGE_URI,
               h.$COL_BMI, h.$COL_CHOLESTEROL, h.$COL_HYPERTENSION, h.$COL_SMOKER,
               h.$COL_HDL, h.$COL_LDL, h.$COL_TRIGLYCERIDES, h.$COL_FBS,
               h.$COL_DIABETES, h.$COL_STROKE_HISTORY, h.$COL_CARDIAC_DISEASE
        FROM $TABLE_USER u
        LEFT JOIN $TABLE_HEALTH_PROFILE h ON u.$COL_USER_ID = h.$COL_USER_ID
        WHERE u.$COL_USER_ID = ?
    """

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            map["name"] = cursor.getString(0) ?: "User"
            map["email"] = cursor.getString(1) ?: "No Email"
            map["age"] = cursor.getString(2) ?: "N/A"
            map["sex"] = cursor.getString(3) ?: "N/A"
            map["image_uri"] = cursor.getString(4) ?: ""
            map["bmi"] = cursor.getString(5) ?: "N/A"
            map["cholesterol"] = cursor.getString(6) ?: "N/A"
            map["hypertension"] = if (cursor.getInt(7) == 1) "Yes" else "No"
            map["smoker"] = if (cursor.getInt(8) == 1) "Yes" else "No"
            map["hdl"] = cursor.getString(9) ?: "N/A"
            map["ldl"] = cursor.getString(10) ?: "N/A"
            map["tri"] = cursor.getString(11) ?: "N/A"
            map["fbs"] = cursor.getString(12) ?: "N/A"
            map["diabetes"] = if (cursor.getInt(13) == 1) "Yes" else "No"
            map["stroke_history"] = if (cursor.getInt(14) == 1) "Yes" else "No"
            map["cardiac_disease"] = if (cursor.getInt(15) == 1) "Yes" else "No"
        }
        cursor.close()
        return map
    }

    // ==========================================
    // ASSESSMENT & YOLO SCAN FUNCTIONS
    // ==========================================

    fun insertRiskAssessment(userId: Long, lrPrediction: Double, riskLevel: String, timestamp: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_LR_PREDICTION, lrPrediction)
            put(COL_RISK_LEVEL, riskLevel)
            put(COL_TIMESTAMP, timestamp)
        }
        val result = db.insert(TABLE_RISK_ASSESSMENT, null, values)
        db.close()
        return result != -1L
    }

    fun getLatestFacialScan(userId: Long): Map<String, Any>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_ASYMMETRIC_DETECTED, $COL_TIMESTAMP FROM $TABLE_SCAN_RESULT WHERE $COL_USER_ID = ? ORDER BY $COL_SCAN_ID DESC LIMIT 1", arrayOf(userId.toString()))
        var scanData: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            scanData = mapOf(
                "detected" to (cursor.getInt(0) == 1),
                "timestamp" to (cursor.getString(1) ?: "Unknown Date"),
                "image_path" to (cursor.getString(2) ?: "")
            )
        }
        cursor.close()
        return scanData
    }

    // ==========================================
    // EMERGENCY CONTACTS FUNCTIONS
    // ==========================================

    // UPDATED: Now accepts name, relationship, and isPrimary
    fun insertEmergencyContact(userId: Long, name: String, relationship: String, isPrimary: Int, phoneNumber: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_CONTACT_NAME, name)
            put(COL_RELATIONSHIP, relationship)
            put(COL_IS_PRIMARY, isPrimary)
            put(COL_PHONE_NUMBER, phoneNumber)
        }
        val result = db.insert(TABLE_EMERGENCY_CONTACTS, null, values)
        db.close()
        return result != -1L
    }

    // UPDATED: Now returns a list of maps containing all the contact details
    fun getEmergencyContacts(userId: Long): List<Map<String, String>> {
        val contactList = mutableListOf<Map<String, String>>()
        val db = this.readableDatabase

        val cursor = db.rawQuery("SELECT $COL_CONTACT_ID, $COL_CONTACT_NAME, $COL_RELATIONSHIP, $COL_IS_PRIMARY, $COL_PHONE_NUMBER FROM $TABLE_EMERGENCY_CONTACTS WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val contactMap = mapOf(
                    "contact_id" to cursor.getString(0),
                    "name" to (cursor.getString(1) ?: ""),
                    "relationship" to (cursor.getString(2) ?: ""),
                    "is_primary" to cursor.getString(3),
                    "phone_number" to (cursor.getString(4) ?: "")
                )
                contactList.add(contactMap)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return contactList
    }

    // UPDATED: Now deletes by the specific contact_id for better accuracy
    fun deleteEmergencyContact(contactId: Long): Boolean {
        val db = this.writableDatabase
        val rows = db.delete(TABLE_EMERGENCY_CONTACTS, "$COL_CONTACT_ID = ?", arrayOf(contactId.toString()))
        db.close()
        return rows > 0
    }

    fun insertAppointment(userId: Long, doctorName: String, aptDate: String, aptTime: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_DOCTOR_NAME, doctorName)
            put(COL_APT_DATE, aptDate)
            put(COL_APT_TIME, aptTime)
        }
        val result = db.insert(TABLE_APPOINTMENTS, null, values)
        db.close()
        return result != -1L
    }

    fun getAppointments(userId: Long): List<Map<String, String>> {
        val aptList = mutableListOf<Map<String, String>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_DOCTOR_NAME, $COL_APT_DATE, $COL_APT_TIME FROM $TABLE_APPOINTMENTS WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                aptList.add(mapOf(
                    "doctor_name" to (cursor.getString(0) ?: ""),
                    "apt_date" to (cursor.getString(1) ?: ""),
                    "apt_time" to (cursor.getString(2) ?: "")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return aptList
    }
    fun getLatestRiskAssessment(userId: Long): Map<String, Any>? {
        val db = this.readableDatabase
        // looks for the newest entry for specific user
        val query = "SELECT lr_prediction, risk_level, timestamp FROM risk_assessments WHERE user_id = ? ORDER BY timestamp DESC LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        var result: Map<String, Any>? = null

        if (cursor.moveToFirst()) {
            result = mapOf(
                "lr_prediction" to cursor.getDouble(cursor.getColumnIndexOrThrow("lr_prediction")),
                "risk_level" to cursor.getString(cursor.getColumnIndexOrThrow("risk_level")),
                "timestamp" to cursor.getString(cursor.getColumnIndexOrThrow("timestamp"))
            )
        }

        cursor.close()
        return result
    }
}