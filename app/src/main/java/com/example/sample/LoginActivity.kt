package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val layoutEmail = findViewById<TextInputLayout>(R.id.layoutEmail)
        val layoutPassword = findViewById<TextInputLayout>(R.id.layoutPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            layoutEmail.error = null
            layoutPassword.error = null

            // We still make sure they typed *something*
            if (email.isEmpty()) {
                layoutEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                layoutPassword.error = "Password is required"
                return@setOnClickListener
            }

            // --- ALL VALIDATION REMOVED ---
            // Any text entered will immediately succeed.

            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

            // Create an Intent to navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)

            // Put the user's email as an extra in the Intent
            intent.putExtra("USER_EMAIL", email)

            startActivity(intent)
            finish() // Prevents going back to the login screen
        }

        // SIGN UP TEXT LOGIC
        tvSignUp.setOnClickListener {
            // Navigate to SignUpActivity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}