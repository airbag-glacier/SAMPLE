package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for 3 seconds (3000ms), then move to LoginActivity
        // In SplashActivity.kt
        Handler(Looper.getMainLooper()).postDelayed({

            // CHANGE THIS LINE:
            val intent = Intent(this, LoginActivity::class.java) // Go to Login first

            startActivity(intent)
            finish()
        }, 3000)
    }


}