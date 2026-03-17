package com.example.sample

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "StrokeApp.db"
        private const val DATABASE_VERSION = 5 // Upgraded to 5 for the Vitals table

        private const val TABLE_RISK_FACTORS = "RiskFactors"
        private const val COL_ID = "id"
        private const val COL_GENDER = "gender"
        private const val COL_AGE = "age"
        private const val COL_HYPERTENSION = "hypertension"
        private const val COL_HEART_DISEASE = "heart_disease"
        private const val COL_EVER_MARRIED = "ever_married"
        private const val COL_WORK_TYPE = "work_type"
        private const val COL_RESIDENCE = "Residence_type"
        private const val COL_GLUCOSE = "avg_glucose_level"
        private const val COL_BMI = "bmi"
        private const val COL_SMOKING = "smoking_status"
        private const val COL_SYNCED = "is_synced_to_cloud"

        private const val TABLE_USERS = "Users"
        private const val COL_EMAIL = "email"
        private const val COL_PASSWORD = "password"
        private const val COL_NAME = "name"
        private const val COL_IMAGE_URI = "image_uri"

        // --- NEW VITALS TABLE CONSTANTS ---
        private const val TABLE_VITALS = "Vitals"
        private const val COL_SYSTOLIC = "systolic"
        private const val COL_DIASTOLIC = "diastolic"
        private const val COL_HEIGHT = "height"
        private const val COL_WEIGHT = "weight"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_RISK_FACTORS ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_GENDER TEXT,"
                + "$COL_AGE REAL,"
                + "$COL_HYPERTENSION INTEGER,"
                + "$COL_HEART_DISEASE INTEGER,"
                + "$COL_EVER_MARRIED TEXT,"
                + "$COL_WORK_TYPE TEXT,"
                + "$COL_RESIDENCE TEXT,"
                + "$COL_GLUCOSE REAL,"
                + "$COL_BMI REAL,"
                + "$COL_SMOKING TEXT,"
                + "$COL_SYNCED INTEGER DEFAULT 0)")
        db.execSQL(createTable)

        val createUsersTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COL_EMAIL TEXT PRIMARY KEY,"
                + "$COL_PASSWORD TEXT,"
                + "$COL_NAME TEXT,"
                + "$COL_IMAGE_URI TEXT)")
        db.execSQL(createUsersTable)

        // --- CREATE VITALS TABLE ---
        val createVitalsTable = ("CREATE TABLE $TABLE_VITALS ("
                + "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COL_SYSTOLIC INTEGER,"
                + "$COL_DIASTOLIC INTEGER,"
                + "$COL_HEIGHT INTEGER,"
                + "$COL_WEIGHT INTEGER)")
        db.execSQL(createVitalsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RISK_FACTORS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VITALS") // Add this
        onCreate(db)
    }

    // --- USER FUNCTIONS ---
    fun registerUser(email: String, password: String, name: String, imageUri: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_EMAIL, email)
            put(COL_PASSWORD, password)
            put(COL_NAME, name)
            put(COL_IMAGE_URI, imageUri)
        }
        val result = db.insert(TABLE_USERS, null, contentValues)
        db.close()
        return result != -1L
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COL_EMAIL=? AND $COL_PASSWORD=?", arrayOf(email, password))
        val count = cursor.count
        cursor.close()
        db.close()
        return count > 0
    }

    fun getUserData(email: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COL_NAME, $COL_IMAGE_URI FROM $TABLE_USERS WHERE $COL_EMAIL=?", arrayOf(email))
        var userData: Map<String, String>? = null
        if (cursor.moveToFirst()) {
            userData = mapOf(
                "name" to cursor.getString(0),
                "image_uri" to cursor.getString(1)
            )
        }
        cursor.close()
        db.close()
        return userData
    }

    // --- RISK FACTOR FUNCTIONS ---
    fun insertRiskFactors(answers: Map<String, Any>): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_GENDER, answers["gender"] as String)
        contentValues.put(COL_AGE, answers["age"] as Double)
        contentValues.put(COL_HYPERTENSION, answers["hypertension"] as Int)
        contentValues.put(COL_HEART_DISEASE, answers["heart_disease"] as Int)
        contentValues.put(COL_EVER_MARRIED, answers["ever_married"] as String)
        contentValues.put(COL_WORK_TYPE, answers["work_type"] as String)
        contentValues.put(COL_RESIDENCE, answers["Residence_type"] as String)
        contentValues.put(COL_GLUCOSE, answers["avg_glucose_level"] as Double)
        contentValues.put(COL_BMI, answers["bmi"] as Double)
        contentValues.put(COL_SMOKING, answers["smoking_status"] as String)
        contentValues.put(COL_SYNCED, 0)

        val result = db.insert(TABLE_RISK_FACTORS, null, contentValues)
        db.close()
        return result != -1L
    }

    // --- NEW: VITALS FUNCTIONS ---
    fun insertVitals(systolic: Int, diastolic: Int, height: Int, weight: Int): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_SYSTOLIC, systolic)
            put(COL_DIASTOLIC, diastolic)
            put(COL_HEIGHT, height)
            put(COL_WEIGHT, weight)
        }
        val result = db.insert(TABLE_VITALS, null, contentValues)
        db.close()
        return result != -1L
    }

    // --- NEW: DYNAMIC SUMMARY GENERATOR ---
    fun getUserHealthSummary(): String {
        val db = this.readableDatabase
        var age = "--"
        var gender = "--"
        var bmi = "--"
        var bp = "--/--"

        // 1. Get Age, Gender, and BMI from the newest RiskFactors entry
        val cursorRisk = db.rawQuery("SELECT $COL_AGE, $COL_GENDER, $COL_BMI FROM $TABLE_RISK_FACTORS ORDER BY $COL_ID DESC LIMIT 1", null)
        if (cursorRisk.moveToFirst()) {
            val ageVal = cursorRisk.getDouble(0)
            age = if (ageVal > 0) ageVal.toInt().toString() else "--" // Removes decimal for age
            gender = cursorRisk.getString(1) ?: "--"
            val bmiVal = cursorRisk.getDouble(2)
            bmi = if (bmiVal > 0) String.format("%.1f", bmiVal) else "--"
        }
        cursorRisk.close()

        // 2. Get Blood Pressure from the newest Vitals entry
        val cursorVitals = db.rawQuery("SELECT $COL_SYSTOLIC, $COL_DIASTOLIC FROM $TABLE_VITALS ORDER BY $COL_ID DESC LIMIT 1", null)
        if (cursorVitals.moveToFirst()) {
            val sys = cursorVitals.getInt(0)
            val dia = cursorVitals.getInt(1)
            bp = "$sys/$dia"
        }
        cursorVitals.close()
        db.close()

        return "$age, $gender | BMI: $bmi | BP: $bp"
    }
}