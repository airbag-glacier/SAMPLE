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

            // MAPPING DATA TO YOUR TABLE
            // We find the specific TextViews in your XML table and set their text individually
            view.findViewById<TextView>(R.id.tvTotalChol).text = profile["cholesterol"]
            view.findViewById<TextView>(R.id.tvHDL).text = profile["hdl"]
            view.findViewById<TextView>(R.id.tvLDL).text = profile["ldl"]
            view.findViewById<TextView>(R.id.tvTriglycerides).text = profile["tri"]
            view.findViewById<TextView>(R.id.tvFbs).text = profile["fbs"]

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

