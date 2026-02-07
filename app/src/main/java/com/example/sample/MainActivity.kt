package com.example.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sample.ui.theme.SAMPLETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home)
        setContent {
            SAMPLETheme {
                Text(text = "App Loaded Successfully!")
            }
        }
    }
}