package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.Activity


class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

            val intent = Intent(this, LoginActivity::class.java)

            startActivity(intent)
            finish()
        }, 3000)
    }


}