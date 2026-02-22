package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // This connects the logic to your existing design
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Camera Button
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            // Navigate to the Scan Screen
            findNavController().navigate(R.id.action_home_to_scan)
        }

        // 2. Setup Checkup Card
        val cardCheckup = view.findViewById<View>(R.id.cardCheckup)
        cardCheckup.setOnClickListener {
            // Navigate to the Checkup Screen
            findNavController().navigate(R.id.action_homeFragment_to_checkupFragment)
        }
    } // <-- This is where the function should properly close
}