package com.example.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This loads activity_main.xml, which contains the NavHostFragment
        setContentView(R.layout.activity_main)
    }
}