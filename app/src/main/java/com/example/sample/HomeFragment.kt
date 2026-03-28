package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
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

        // Camera Button
        view.findViewById<FloatingActionButton>(R.id.btnCamera)?.setOnClickListener {
            findNavController().navigate(R.id.action_global_scan)
        }

        // Bottom Navigation Menu
        view.findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            val menuOptions = arrayOf("Assessment Result", "Emergency Contacts", "About / Credits", "Log out")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("DeTechStroke Menu")
                .setItems(menuOptions) { _, which ->
                    when (which) {
                        0 -> findNavController().navigate(R.id.action_global_assessmentResult)
                        1 -> findNavController().navigate(R.id.action_global_emergencyContacts)
                        2 -> {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("DeTechStroke")
                                .setMessage("Developers:\nGabriel Garcia\nPhoebe Andrei Quan\nNatsuki Ushijima\n\n© 2026 All Rights Reserved.")
                                .setPositiveButton("Close", null)
                                .show()
                        }
                        3 -> {
                            val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                requireActivity().finish()
                            }
                        }
                    }
                }
                .setNegativeButton("Close Menu", null)
                .show()
        }

        // Standard Cards
        view.findViewById<View>(R.id.cardCheckup)?.setOnClickListener { findNavController().navigate(R.id.action_homeFragment_to_checkupFragment) }
        view.findViewById<View>(R.id.cardVitals)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_vitals) }
        view.findViewById<View>(R.id.cardBefast)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_befast) }
        view.findViewById<View>(R.id.cardBloodChem)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_bloodChem) }
        view.findViewById<View>(R.id.cardRiskFactors)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_riskFactors) }
        view.findViewById<View>(R.id.tvSeeDetails)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_profileDetails) }


// ==========================================
        // THE POSITION-BASED FAIL-SAFE (No IDs needed)
        // ==========================================
        try {
            val rootLayout = view.findViewById<ViewGroup>(R.id.headerLayout).parent as ViewGroup
            val scrollView = rootLayout.getChildAt(1) as? androidx.core.widget.NestedScrollView
            val scrollContainer = scrollView?.getChildAt(0) as? LinearLayout

            // We search every single view in your Home Screen manually
            if (scrollContainer != null) {
                for (i in 0 until scrollContainer.childCount) {
                    val child = scrollContainer.getChildAt(i)

                    // Look inside the HorizontalScrollViews
                    if (child is HorizontalScrollView) {
                        val innerLayout = child.getChildAt(0) as? LinearLayout
                        innerLayout?.let {
                            for (j in 0 until it.childCount) {
                                val card = it.getChildAt(j) as? com.google.android.material.card.MaterialCardView

                                // Check if this card contains the "Hospitals" text
                                val cardContent = (card?.getChildAt(0) as? ViewGroup)
                                for (k in 0 until (cardContent?.childCount ?: 0)) {
                                    val innerView = cardContent?.getChildAt(k)
                                    if (innerView is TextView && innerView.text.contains("Hospitals", ignoreCase = true)) {
                                        // WE FOUND IT! Attach the click listener directly to the object
                                        card.setOnClickListener {
                                            findNavController().navigate(R.id.hospitalMapFragment)
                                        }
                                        // Also make the image inside it clickable just in case
                                        innerView.setOnClickListener {
                                            findNavController().navigate(R.id.hospitalMapFragment)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail if the structure is different, but this won't show an "Invisible" error
        }

        // Database and User Profile Logic
        val dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent?.getLongExtra("USER_ID", -1L) ?: -1L

        if (userId != -1L) {
            val userData = dbHelper.getUserData(userId)
            if (userData != null) {
                view.findViewById<TextView>(R.id.tvName).text = userData["name"]

                val imageUriString = userData["image_uri"]
                val profileImageView = view.findViewById<ImageView>(R.id.imgProfile)

                if (!imageUriString.isNullOrEmpty()) {
                    try {
                        profileImageView.setImageURI(imageUriString.toUri())
                    } catch (e: SecurityException) {
                        profileImageView.setImageResource(R.drawable.pink_profile_image)
                    }
                }
            }
            view.findViewById<TextView>(R.id.tvDetails).text = dbHelper.getUserHealthSummary(userId)
        }
    }
}