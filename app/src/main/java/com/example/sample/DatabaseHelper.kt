package com.example.sample

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "StrokeApp.db"
        // Version 3 triggers a table refresh for the new Kaggle columns
        private const val DATABASE_VERSION = 3
        private const val TABLE_RISK_FACTORS = "RiskFactors"

        // Kaggle Dataset Exact Columns
        private const val COL_ID = "id"
        private const val COL_GENDER = "gender" // TEXT
        private const val COL_AGE = "age" // REAL
        private const val COL_HYPERTENSION = "hypertension" // INTEGER (0 or 1)
        private const val COL_HEART_DISEASE = "heart_disease" // INTEGER (0 or 1)
        private const val COL_EVER_MARRIED = "ever_married" // TEXT (Yes or No)
        private const val COL_WORK_TYPE = "work_type" // TEXT
        private const val COL_RESIDENCE = "Residence_type" // TEXT (Urban or Rural)
        private const val COL_GLUCOSE = "avg_glucose_level" // REAL
        private const val COL_BMI = "bmi" // REAL
        private const val COL_SMOKING = "smoking_status" // TEXT
        private const val COL_SYNCED = "is_synced_to_cloud"
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RISK_FACTORS")
        onCreate(db)
    }

    // Accepts a mix of Strings, Ints, and Doubles using 'Any'
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
}