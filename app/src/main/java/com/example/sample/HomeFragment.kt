package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<FloatingActionButton>(R.id.btnCamera)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }

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

        view.findViewById<View>(R.id.cardCheckup)?.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_checkupFragment) }
        view.findViewById<View>(R.id.cardVitals)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_vitals) }
        view.findViewById<View>(R.id.cardBefast)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_befast) }
        view.findViewById<View>(R.id.cardBloodChem)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_bloodChem) }
        view.findViewById<View>(R.id.cardRiskFactors)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_riskFactors) }
        view.findViewById<TextView>(R.id.tvSeeDetails)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_profileDetails) }

        // FIX: Use USER_ID (Long) instead of Email
        val dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent?.getLongExtra("USER_ID", -1L) ?: -1L

        if (userId != -1L) {
            val userData = dbHelper.getUserData(userId)
            if (userData != null) {
                view.findViewById<TextView>(R.id.tvName).text = userData["name"]
                val imageUriString = userData["image_uri"]
                if (!imageUriString.isNullOrEmpty()) {
                    view.findViewById<ImageView>(R.id.imgProfile).setImageURI(imageUriString.toUri())
                }
            }
            // FIX: Pass userId to health summary
            view.findViewById<TextView>(R.id.tvDetails).text = dbHelper.getUserHealthSummary(userId)
        }
    }
}