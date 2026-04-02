package com.example.sample

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DeTechStroke.db"
        private const val DATABASE_VERSION = 8

        // --- 1. USER TABLE ---
        private const val TABLE_USER = "User"
        private const val COL_USER_ID = "user_id"
        private const val COL_USER_NAME = "user_name"
        private const val COL_EMAIL = "email"
        private const val COL_PASSWORD = "password"
        private const val COL_IMAGE_URI = "image_uri"
        private const val COL_AGE = "age"
        private const val COL_SEX = "sex"

        // --- 2. HEALTH RISK FACTOR PROFILE TABLE () ---
        private const val TABLE_HEALTH_PROFILE = "HealthRiskFactorProfile"
        private const val COL_PROFILE_ID = "profile_id"
        private const val COL_HYPERTENSION = "hypertension"
        private const val COL_DIABETES = "diabetes"
        private const val COL_SMOKER = "smoker"
        private const val COL_CARDIAC_DISEASE = "cardiac_disease"
        private const val COL_BMI = "bmi"
        private const val COL_HDL = "hdl_level"
        private const val COL_LDL = "ldl_level"
        private const val COL_TRIGLYCERIDES = "triglycerides"


        private const val TABLE_EXTENDED = "ExtendedMetrics"
        private const val COL_EXT_ID = "ext_id"
        private const val COL_HEIGHT = "height"
        private const val COL_WEIGHT = "weight"
        private const val COL_TOTAL_CHOL = "cholesterol"
        private const val COL_FBS = "fbs"

        // --- OTHER TABLES ---
        private const val TABLE_EMERGENCY_CONTACTS = "EmergencyContacts"
        private const val COL_CONTACT_ID = "contact_id"
        private const val COL_CONTACT_NAME = "name"
        private const val COL_RELATIONSHIP = "relationship"
        private const val COL_IS_PRIMARY = "is_primary"
        private const val COL_PHONE_NUMBER = "phone_number"

        private const val TABLE_SCAN_RESULT = "FacialScanResult"
        private const val COL_SCAN_ID = "scan_id"
        private const val COL_ASYMMETRIC_DETECTED = "asymmetric_detected"
        private const val COL_CONFIDENCE = "confidence"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_IMAGE_PATH = "image_path"

        private const val TABLE_RISK_ASSESSMENT = "risk_assessments"
        private const val COL_RISK_ID = "risk_id"
        private const val COL_LR_PREDICTION = "lr_prediction"
        private const val COL_RISK_LEVEL = "risk_level"

        private const val TABLE_APPOINTMENTS = "Appointments"
        private const val COL_APT_ID = "apt_id"
        private const val COL_DOCTOR_NAME = "doctor_name"
        private const val COL_APT_DATE = "apt_date"
        private const val COL_APT_TIME = "apt_time"
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
                + "$COL_CARDIAC_DISEASE INTEGER,"
                + "$COL_BMI REAL,"
                + "$COL_HDL REAL,"
                + "$COL_LDL REAL,"
                + "$COL_TRIGLYCERIDES REAL,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createHealthProfileTable)


        val createExtendedTable = ("CREATE TABLE $TABLE_EXTENDED ("
                + "$COL_EXT_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER UNIQUE,"
                + "$COL_HEIGHT REAL,"
                + "$COL_WEIGHT REAL,"
                + "$COL_TOTAL_CHOL REAL,"
                + "$COL_FBS REAL,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createExtendedTable)

        val createEmergencyContactsTable = ("CREATE TABLE $TABLE_EMERGENCY_CONTACTS ("
                + "$COL_CONTACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_USER_ID INTEGER,"
                + "$COL_CONTACT_NAME TEXT,"
                + "$COL_RELATIONSHIP TEXT,"
                + "$COL_IS_PRIMARY INTEGER DEFAULT 0,"
                + "$COL_PHONE_NUMBER TEXT,"
                + "FOREIGN KEY($COL_USER_ID) REFERENCES $TABLE_USER($COL_USER_ID) ON DELETE CASCADE)")
        db.execSQL(createEmergencyContactsTable)

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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EMERGENCY_CONTACTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXTENDED") // Drop shadow table
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
    // ERD & SHADOW DATA MAPPING FUNCTIONS
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

    private fun ensureExtendedExists(userId: Long) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $COL_EXT_ID FROM $TABLE_EXTENDED WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))
        if (!cursor.moveToFirst()) {
            val values = ContentValues().apply { put(COL_USER_ID, userId) }
            db.insert(TABLE_EXTENDED, null, values)
        }
        cursor.close()
    }

    fun updateVitalsToERD(userId: Long, height: Double, weight: Double, bmi: Double): Boolean {
        ensureProfileExists(userId)
        ensureExtendedExists(userId)
        val db = this.writableDatabase


        val erdValues = ContentValues().apply { put(COL_BMI, bmi) }
        val rows = db.update(TABLE_HEALTH_PROFILE, erdValues, "$COL_USER_ID = ?", arrayOf(userId.toString()))


        val extValues = ContentValues().apply {
            put(COL_HEIGHT, height)
            put(COL_WEIGHT, weight)
        }
        db.update(TABLE_EXTENDED, extValues, "$COL_USER_ID = ?", arrayOf(userId.toString()))

        db.close()
        return rows > 0
    }

    fun updateBloodChemToERD(userId: Long, totalChol: Double, hdl: Double, ldl: Double, tri: Double, fbs: Double): Boolean {
        ensureProfileExists(userId)
        ensureExtendedExists(userId)
        val db = this.writableDatabase

        // 1. Update ERD Table
        val erdValues = ContentValues().apply {
            put(COL_HDL, hdl)
            put(COL_LDL, ldl)
            put(COL_TRIGLYCERIDES, tri)
        }
        val rows = db.update(TABLE_HEALTH_PROFILE, erdValues, "$COL_USER_ID = ?", arrayOf(userId.toString()))

        // 2. Secretly update Shadow Table
        val extValues = ContentValues().apply {
            put(COL_TOTAL_CHOL, totalChol)
            put(COL_FBS, fbs)
        }
        db.update(TABLE_EXTENDED, extValues, "$COL_USER_ID = ?", arrayOf(userId.toString()))

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


        val keys = listOf("name", "email", "password", "age", "sex", "bmi", "hdl", "ldl", "tri", "hypertension", "smoker", "diabetes", "cardiac_disease", "height", "weight", "cholesterol", "fbs", "image_uri")
        keys.forEach { map[it] = "N/A" }
        map["image_uri"] = ""


        val query = """
        SELECT u.$COL_USER_NAME, u.$COL_EMAIL, u.$COL_PASSWORD, u.$COL_AGE, u.$COL_SEX, u.$COL_IMAGE_URI,
               h.$COL_BMI, h.$COL_HYPERTENSION, h.$COL_SMOKER,
               h.$COL_HDL, h.$COL_LDL, h.$COL_TRIGLYCERIDES,
               h.$COL_DIABETES, h.$COL_CARDIAC_DISEASE,
               e.$COL_HEIGHT, e.$COL_WEIGHT, e.$COL_TOTAL_CHOL, e.$COL_FBS
        FROM $TABLE_USER u
        LEFT JOIN $TABLE_HEALTH_PROFILE h ON u.$COL_USER_ID = h.$COL_USER_ID
        LEFT JOIN $TABLE_EXTENDED e ON u.$COL_USER_ID = e.$COL_USER_ID
        WHERE u.$COL_USER_ID = ?
        """

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            map["name"] = cursor.getString(0) ?: "User"
            map["email"] = cursor.getString(1) ?: "No Email"
            map["password"] = cursor.getString(2) ?: "No Password"
            map["age"] = cursor.getString(3) ?: "N/A"
            map["sex"] = cursor.getString(4) ?: "N/A"
            map["image_uri"] = cursor.getString(5) ?: ""
            map["bmi"] = cursor.getString(6) ?: "N/A"
            map["hypertension"] = if (cursor.getInt(7) == 1) "Yes" else "No"
            map["smoker"] = if (cursor.getInt(8) == 1) "Yes" else "No"
            map["hdl"] = cursor.getString(9) ?: "N/A"
            map["ldl"] = cursor.getString(10) ?: "N/A"
            map["tri"] = cursor.getString(11) ?: "N/A"
            map["diabetes"] = if (cursor.getInt(12) == 1) "Yes" else "No"
            map["cardiac_disease"] = if (cursor.getInt(13) == 1) "Yes" else "No"

            // Extract the Shadow Table data!
            map["height"] = cursor.getString(14) ?: "N/A"
            map["weight"] = cursor.getString(15) ?: "N/A"
            map["cholesterol"] = cursor.getString(16) ?: "N/A"
            map["fbs"] = cursor.getString(17) ?: "N/A"
        }
        cursor.close()
        return map
    }

    // ==========================================
    // ASSESSMENT, SCANS, & APPOINTMENTS
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
        val query = "SELECT $COL_ASYMMETRIC_DETECTED, $COL_TIMESTAMP, $COL_IMAGE_PATH FROM $TABLE_SCAN_RESULT WHERE $COL_USER_ID = ? ORDER BY $COL_SCAN_ID DESC LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
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

    fun clearPrimaryContact(userId: Long): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply { put(COL_IS_PRIMARY, 0) }
        val rows = db.update(TABLE_EMERGENCY_CONTACTS, values, "$COL_USER_ID = ?", arrayOf(userId.toString()))
        db.close()
        return rows > 0
    }

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
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $COL_APT_ID, $COL_APT_DATE, $COL_APT_TIME FROM $TABLE_APPOINTMENTS WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))
        val formatter = SimpleDateFormat("M/d/yyyy hh:mm a", Locale.US)
        val currentTime = Date()

        if (cursor.moveToFirst()) {
            do {
                val aptId = cursor.getLong(0)
                val aptDate = cursor.getString(1) ?: ""
                val aptTime = cursor.getString(2) ?: ""

                try {
                    val appointmentDate = formatter.parse("$aptDate $aptTime")
                    if (appointmentDate != null && appointmentDate.before(currentTime)) {
                        db.delete(TABLE_APPOINTMENTS, "$COL_APT_ID = ?", arrayOf(aptId.toString()))
                    }
                } catch (e: Exception) {}
            } while (cursor.moveToNext())
        }
        cursor.close()

        val aptList = mutableListOf<Map<String, String>>()
        val validCursor = db.rawQuery("SELECT $COL_DOCTOR_NAME, $COL_APT_DATE, $COL_APT_TIME FROM $TABLE_APPOINTMENTS WHERE $COL_USER_ID = ?", arrayOf(userId.toString()))

        if (validCursor.moveToFirst()) {
            do {
                aptList.add(mapOf(
                    "doctor_name" to (validCursor.getString(0) ?: ""),
                    "apt_date" to (validCursor.getString(1) ?: ""),
                    "apt_time" to (validCursor.getString(2) ?: "")
                ))
            } while (validCursor.moveToNext())
        }
        validCursor.close()

        return aptList
    }

    fun getLatestRiskAssessment(userId: Long): Map<String, Any>? {
        val db = this.readableDatabase
        val query = "SELECT lr_prediction, risk_level, timestamp FROM risk_assessments WHERE user_id = ? ORDER BY timestamp DESC LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var result: Map<String, Any>? = null

        if (cursor.moveToFirst()) {
            result = mapOf(
                "lr_prediction" to cursor.getDouble(0),
                "risk_level" to (cursor.getString(1) ?: "Unknown"),
                "timestamp" to (cursor.getString(2) ?: "Unknown Date")
            )
        }
        cursor.close()
        return result
    }

    fun getAllRiskAssessments(userId: Long): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT lr_prediction, risk_level, timestamp FROM risk_assessments WHERE user_id = ? ORDER BY timestamp DESC", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                list.add(mapOf(
                    "lr_prediction" to cursor.getDouble(0),
                    "risk_level" to (cursor.getString(1) ?: "Unknown"),
                    "timestamp" to (cursor.getString(2) ?: "Unknown Date")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getAllFacialScans(userId: Long): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_ASYMMETRIC_DETECTED, $COL_TIMESTAMP FROM $TABLE_SCAN_RESULT WHERE $COL_USER_ID = ? ORDER BY $COL_TIMESTAMP DESC", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                list.add(mapOf(
                    "detected" to (cursor.getInt(0) == 1),
                    "timestamp" to (cursor.getString(1) ?: "Unknown Date")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}