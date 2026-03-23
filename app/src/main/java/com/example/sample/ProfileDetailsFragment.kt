package com.example.sample

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        val dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        if (userId != -1L) {
            val profile = dbHelper.getFullUserProfile(userId)

            // Basic Info
            view.findViewById<TextView>(R.id.tvFullName).text = profile["name"]
            view.findViewById<TextView>(R.id.tvEmail).text = profile["email"]
            view.findViewById<TextView>(R.id.tvAgeGender).text = "Age / Gender: ${profile["age"]} / ${profile["sex"]}"

            // Vitals & History
            view.findViewById<TextView>(R.id.tvFullBmi).text = "BMI: ${profile["bmi"]}"
            view.findViewById<TextView>(R.id.tvFullBp).text = "Hypertension: ${profile["hypertension"]}"
            view.findViewById<TextView>(R.id.tvSmoking).text = "Smoker: ${profile["smoker"]}"

// MAPPING DATA FOR BLOOD CHEM TABLE
            view.findViewById<TextView>(R.id.tvTotalChol).text = "Total Cholesterol: ${profile["cholesterol"]} mg/dL"
            view.findViewById<TextView>(R.id.tvHDL).text = "HDL: ${profile["hdl"]} mg/dL"
            view.findViewById<TextView>(R.id.tvLDL).text = "LDL: ${profile["ldl"]} mg/dL"
            view.findViewById<TextView>(R.id.tvTriglycerides).text = "Triglycerides: ${profile["tri"]} mg/dL"
            view.findViewById<TextView>(R.id.tvFbs).text = "Fasting Blood Sugar: ${profile["fbs"]} mg/dL"

            // Image handling
            val imageUriString = profile["image_uri"]
            val profileImageView = view.findViewById<ImageView>(R.id.imgFullProfile)
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    profileImageView.setImageURI(Uri.parse(imageUriString))
                } catch (e: SecurityException) {
                    profileImageView.setImageResource(R.drawable.pink_profile_image)
                }
            }
        }
        }
}

