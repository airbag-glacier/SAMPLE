package com.example.sample

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Get SharedPreferences instance [1]
                val sharedPref = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()

                // Save the password with the email as the key [9]
                editor.putString(email, password)
                editor.apply() // Use apply() for asynchronous saving [1]

                Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                finish() // Go back to the LoginActivity
            } else {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
