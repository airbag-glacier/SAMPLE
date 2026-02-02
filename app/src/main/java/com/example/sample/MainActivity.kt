package com.example.sample // Ensure this matches your package name at the very top!

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity // This is what your project uses!
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sample.ui.theme.SAMPLETheme // Ensure this matches your theme name

class MainActivity : ComponentActivity() { // changed from AppCompatActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen (Must be the very first line)
        val splashScreen = installSplashScreen()

        // 2. Add the 3-second delay logic
        var keepSplashOnScreen = true
        val delay = 3000L
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
        }, delay)
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 3. Custom Exit Animation (Fade Out)
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            )
            slideUp.interpolator = OvershootInterpolator()
            slideUp.duration = 1000L
            slideUp.doOnEnd { splashScreenView.remove() }
            slideUp.start()
        }

        // 4. The Content (This replaces setContentView)
        setContent {
            // Note: If 'SAMPLETheme' is red, look inside your ui/theme/Theme.kt file to check the name
            SAMPLETheme {
                // This is just a placeholder to prove the app loads
                Text(text = "App Loaded Successfully!")
            }
        }
    }
}