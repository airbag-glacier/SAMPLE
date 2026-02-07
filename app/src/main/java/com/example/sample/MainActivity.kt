package com.example.sample // Check your package name

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen (Android 12+ style)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. LINK THE FRAME (activity_main), NOT THE PHOTO (fragment_home)
        setContentView(R.layout.activity_main)
    }
}