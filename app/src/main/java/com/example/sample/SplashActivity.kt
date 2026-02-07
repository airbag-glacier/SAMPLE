package com.example.sample // Make sure this matches your package name!

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // The Delay Logic (7000 milliseconds = 7 seconds)
        Handler(Looper.getMainLooper()).postDelayed({

            // 1. Create intent to go to the Dashboard (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // 2. Close this Splash Screen so the user can't go back to it
            finish()

        }, 7000)
    }
}