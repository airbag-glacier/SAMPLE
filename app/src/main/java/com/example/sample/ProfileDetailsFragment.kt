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

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        val dbHelper = DatabaseHelper(requireContext())

        // Retrieve USER_ID instead of Email
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        if (userId != -1L) {
            val profile = dbHelper.getFullUserProfile(userId)

            view.findViewById<TextView>(R.id.tvFullName).text = profile["name"]
            view.findViewById<TextView>(R.id.tvEmail).text = profile["email"]

            view.findViewById<TextView>(R.id.tvAgeGender).text = "Age / Gender: ${profile["age"]} / ${profile["sex"]}"

            // The ERD no longer stores height/weight/bp natively, so we show what is in the ERD
            view.findViewById<TextView>(R.id.tvHeightWeight).text = "Cholesterol: ${profile["cholesterol"]}"
            view.findViewById<TextView>(R.id.tvFullBmi).text = "BMI: ${profile["bmi"]}"
            view.findViewById<TextView>(R.id.tvFullBp).text = "Hypertension: ${profile["hypertension"]}"
            view.findViewById<TextView>(R.id.tvSmoking).text = "Smoker: ${profile["smoker"]}"

            val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

            if (userId != -1L) {
                val dbHelper = DatabaseHelper(requireContext())

                val userProfile = dbHelper.getFullUserProfile(userId)




                val imageUriString = userProfile["image_uri"]
                if (!imageUriString.isNullOrEmpty()) {
                    // FIXED: Changed R.id.imgProfile to R.id.imgFullProfile
                    val profileImageView = view.findViewById<ImageView>(R.id.imgFullProfile)

                    try {
                        profileImageView.setImageURI(android.net.Uri.parse(imageUriString))
                    } catch (e: SecurityException) {
                        profileImageView.setImageResource(R.drawable.pink_profile_image)
                    }
                }
            }
        }
    }
}