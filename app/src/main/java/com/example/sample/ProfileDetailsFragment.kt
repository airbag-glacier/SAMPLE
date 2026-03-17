package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
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

        // Setup Back Button to return to Home Screen
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        val dbHelper = DatabaseHelper(requireContext())
        val userEmail = requireActivity().intent.getStringExtra("USER_EMAIL")

        if (userEmail != null) {
            val profile = dbHelper.getFullUserProfile(userEmail)

            // Populate Text Fields
            view.findViewById<TextView>(R.id.tvFullName).text = profile["name"]
            view.findViewById<TextView>(R.id.tvEmail).text = profile["email"]

            view.findViewById<TextView>(R.id.tvAgeGender).text = "Age / Gender: ${profile["age"]} / ${profile["gender"]}"
            view.findViewById<TextView>(R.id.tvHeightWeight).text = "Height / Weight: ${profile["height"]} cm / ${profile["weight"]} kg"

            view.findViewById<TextView>(R.id.tvFullBmi).text = "BMI: ${profile["bmi"]}"
            view.findViewById<TextView>(R.id.tvFullBp).text = "Blood Pressure: ${profile["bp"]}"
            view.findViewById<TextView>(R.id.tvSmoking).text = "Smoking Status: ${profile["smoking"]}"

            // Load Profile Image
            val imageUriString = profile["image_uri"]
            if (!imageUriString.isNullOrEmpty()) {
                view.findViewById<ImageView>(R.id.imgFullProfile).setImageURI(imageUriString.toUri())
            }
        }
    }
}