package com.example.sample

import android.content.Context
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

            if (email.isEmpty()) {
                layoutEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                layoutPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Get SharedPreferences instance
            val sharedPref = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)

            // Retrieve the saved password for the entered email.
            // The second argument is a default value if the key is not found.
            val savedPassword = sharedPref.getString(email, null)

            // Check if the saved password matches the entered password
            if (savedPassword == password) {
                // Navigate to Main Activity
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Prevents going back to the login screen
            } else {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }

            if (savedPassword == password) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                // Create an Intent to navigate to MainActivity
                val intent = Intent(this, MainActivity::class.java)


                // Put the user's email as an extra in the Intent.
                // We'll use this email as the display name for now.
                intent.putExtra("USER_EMAIL", email)

                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }

        }

        // SIGN UP TEXT LOGIC
        tvSignUp.setOnClickListener {
            // Navigate to SignUpActivity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
