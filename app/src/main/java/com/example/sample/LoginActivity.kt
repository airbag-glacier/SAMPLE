package com.example.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private var loadingDialog: AlertDialog? = null


    private fun getBaseUrl(): String {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getString("SERVER_IP", "http://192.168.1.15:5000/")!!
    }

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

            showLoadingScreen()
            performLoginAndSync(email, password)
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Long-pressing the "Sign Up" text opens the IP Configuration
        tvSignUp.setOnLongClickListener {
            showIpConfigDialog()
            true
        }
    }

    // ==========================================
    // DEVELOPER CONFIGURATION
    // ==========================================
    private fun showIpConfigDialog() {

        val currentIp = getBaseUrl().replace("http://", "").replace(":5000/", "")

        val input = EditText(this).apply {
            setText(currentIp)
            setPadding(50, 40, 50, 40)
            hint = "e.g., 192.168.1.15"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Developer: Set Server IP")
            .setMessage("Enter the laptop's current IPv4 address.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newIp = input.text.toString().trim()
                if (newIp.isNotEmpty()) {
                    val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("SERVER_IP", "http://$newIp:5000/").apply()
                    Toast.makeText(this, "Server IP updated to: $newIp", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // LOADING SCREEN LOGIC
    // ==========================================
    private fun showLoadingScreen() {
        val progressBar = ProgressBar(this).apply { setPadding(0, 50, 0, 50) }
        loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Authenticating")
            .setMessage("Connecting to ${getBaseUrl()}...") // Shows the active IP!
            .setView(progressBar)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun hideLoadingScreen() {
        loadingDialog?.dismiss()
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    private fun performLoginAndSync(emailInput: String, passwordInput: String) {
        val credentials = LoginCredentials(email = emailInput, passwordHash = passwordInput)


        val retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
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
                        val dbHelper = DatabaseHelper(this@LoginActivity)
                        val localUserId = dbHelper.authenticateUser(emailInput, passwordInput)
                        hideLoadingScreen()
                        navigateToHome(if (localUserId != -1L) localUserId else -1L)
                    }
                } else {
                    hideLoadingScreen()
                    Toast.makeText(this@LoginActivity, "Login Failed: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                val dbHelper = DatabaseHelper(this@LoginActivity)
                val localUserId = dbHelper.authenticateUser(emailInput, passwordInput)
                hideLoadingScreen()

                if (localUserId != -1L) {
                    Toast.makeText(this@LoginActivity, "Server unreachable. Logged in offline.", Toast.LENGTH_LONG).show()
                    navigateToHome(localUserId)
                } else {
                    Toast.makeText(this@LoginActivity, "Offline Login Failed. Check connection.", Toast.LENGTH_LONG).show()
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

        hideLoadingScreen()
        Toast.makeText(this, "Account restored successfully!", Toast.LENGTH_SHORT).show()
        navigateToHome(userId)
    }

    private fun navigateToHome(userId: Long) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
        finish()
    }
}