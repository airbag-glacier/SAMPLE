package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private val baseUrl = "http://192.168.1.15:5000/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TRIGGER THE CLOUD SYNC INSTEAD OF JUST LOCAL AUTH
            Toast.makeText(this, "Connecting to server...", Toast.LENGTH_SHORT).show()
            performLoginAndSync(email, password)
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLoginAndSync(emailInput: String, passwordInput: String) {
        val credentials = LoginCredentials(email = emailInput, passwordHash = passwordInput)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CloudSyncApi::class.java)

        api.loginUser(credentials).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val payload = response.body()?.userData

                    if (payload != null) {
                        rebuildLocalDatabase(payload)
                    } else {
                        // Fallback if cloud login succeeds but has no saved data yet
                        val dbHelper = DatabaseHelper(this@LoginActivity)
                        val localUserId = dbHelper.authenticateUser(emailInput, passwordInput)
                        navigateToHome(if (localUserId != -1L) localUserId else -1L)
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login Failed: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // OFFLINE-FIRST FALLBACK: If there's no internet, try to log in locally!
                Toast.makeText(this@LoginActivity, "No internet. Trying offline login...", Toast.LENGTH_SHORT).show()

                val dbHelper = DatabaseHelper(this@LoginActivity)
                val localUserId = dbHelper.authenticateUser(emailInput, passwordInput)

                if (localUserId != -1L) {
                    navigateToHome(localUserId)
                } else {
                    Toast.makeText(this@LoginActivity, "Offline Login Failed. Check credentials.", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun rebuildLocalDatabase(cloudData: CloudSyncPayload) {
        val dbHelper = DatabaseHelper(this)
        val userId = cloudData.userId

        Log.d("SyncDown", "Rebuilding local database for User ID: $userId")

        cloudData.userProfile?.let { profile ->
            dbHelper.updateRiskFactorsToERD(
                userId = userId,
                age = profile["age"]?.toIntOrNull() ?: 0,
                hypertension = if (profile["hypertension"] == "Yes") 1 else 0,
                cardiacDisease = if (profile["heart_disease"] == "Yes") 1 else 0,
                bmi = profile["bmi"]?.toDoubleOrNull() ?: 0.0,
                smoker = if (profile["smoker"] == "Yes") 1 else 0,
                diabetes = 0
            )
        }

        cloudData.emergencyContacts.forEach { contact ->
            dbHelper.insertEmergencyContact(
                userId = userId,
                name = contact["name"] ?: "",
                relationship = contact["relationship"] ?: "",
                isPrimary = contact["is_primary"]?.toIntOrNull() ?: 0,
                phoneNumber = contact["phone_number"] ?: ""
            )
        }

        Toast.makeText(this, "Account restored successfully!", Toast.LENGTH_SHORT).show()
        navigateToHome(userId)
    }

    private fun navigateToHome(userId: Long) {
        // Change MainActivity::class.java to whatever your Dashboard/Host Activity is named!
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
        finish()
    }
}