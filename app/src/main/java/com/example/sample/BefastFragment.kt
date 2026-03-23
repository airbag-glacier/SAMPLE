package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BefastFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_befast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Top Back Button Logic
        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            findNavController().popBackStack()
        }

        // 2. Bottom Navigation: Camera (Global Action)
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_global_scan)
        }

        // 3. Bottom Navigation: Home
        val btnHome = view.findViewById<ImageView>(R.id.btnHome)
        btnHome?.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        // 4. Bottom Navigation: Menu Dialog
        view.findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("DeTechStroke")
                .setMessage("Developers:\nGabriel Garcia\nPhoebe Andrei Quan\nNatsuki Ushijima\n\n© 2026 All Rights Reserved.")
                .setPositiveButton("Restart App") { _, _ ->
                    val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}