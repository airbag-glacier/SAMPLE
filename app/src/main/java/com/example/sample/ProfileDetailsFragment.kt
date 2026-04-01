package com.example.sample

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileDetailsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper

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

        dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        if (userId != -1L) {
            val profile = dbHelper.getFullUserProfile(userId)

            // Basic Info
            view.findViewById<TextView>(R.id.tvFullName).text = profile["name"]
            view.findViewById<TextView>(R.id.tvEmail).text = profile["email"]


            val ageStr = if (profile["age"] == "0" || profile["age"] == "N/A" || profile["age"] == "null") "N/A" else profile["age"]
            val genderStr = if (profile["sex"] == "null" || profile["sex"] == "N/A" || profile["sex"].isNullOrEmpty()) "N/A" else profile["sex"]
            view.findViewById<TextView>(R.id.tvAgeGender).text = "Age / Gender: $ageStr / $genderStr"


            val hStr = if (profile["height"] == "0.0" || profile["height"] == "N/A" || profile["height"] == "null") "-" else profile["height"]
            val wStr = if (profile["weight"] == "0.0" || profile["weight"] == "N/A" || profile["weight"] == "null") "-" else profile["weight"]

            // Check your XML file to make sure this ID matches your Height/Weight textview!
            view.findViewById<TextView>(R.id.tvHeightWeight)?.text = "Height / Weight: $hStr cm / $wStr kg"

            // Vitals & History
            view.findViewById<TextView>(R.id.tvFullBmi).text = "BMI: ${profile["bmi"]}"
            view.findViewById<TextView>(R.id.tvFullBp).text = "Hypertension: ${profile["hypertension"]}"
            view.findViewById<TextView>(R.id.tvSmoking).text = "Smoker: ${profile["smoker"]}"

            // Blood Chemistry
            view.findViewById<TextView>(R.id.tvTotalChol).text = "Total Cholesterol: ${profile["cholesterol"]} mg/dL"
            view.findViewById<TextView>(R.id.tvHDL).text = "HDL: ${profile["hdl"]} mg/dL"
            view.findViewById<TextView>(R.id.tvLDL).text = "LDL: ${profile["ldl"]} mg/dL"
            view.findViewById<TextView>(R.id.tvTriglycerides).text = "Triglycerides: ${profile["tri"]} mg/dL"
            view.findViewById<TextView>(R.id.tvFbs).text = "Fasting Blood Sugar: ${profile["fbs"]} mg/dL"

            // Profile Picture
            val imageUriString = profile["image_uri"]
            val profileImageView = view.findViewById<ImageView>(R.id.imgFullProfile)
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    profileImageView.setImageURI(Uri.parse(imageUriString))
                } catch (e: SecurityException) {
                    profileImageView.setImageResource(R.drawable.pink_profile_image)
                }
            }

            loadRiskHistory(userId)
            loadScanHistory(userId)
            loadAppointments(userId)
        }
    }

    private fun loadRiskHistory(userId: Long) {
        val historyLayout = view?.findViewById<LinearLayout>(R.id.llRiskHistoryList)
        val emptyText = view?.findViewById<TextView>(R.id.tvEmptyRiskHistory)
        val pastRisks = dbHelper.getAllRiskAssessments(userId)

        if (pastRisks.isNotEmpty()) {
            emptyText?.visibility = View.GONE
            for (risk in pastRisks) {
                val resultView = TextView(requireContext()).apply {
                    text = "• ${risk["timestamp"]}: ${risk["risk_level"]} (${risk["lr_prediction"]}%)"
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                }
                historyLayout?.addView(resultView)
            }
        }
    }

    private fun loadScanHistory(userId: Long) {
        val historyLayout = view?.findViewById<LinearLayout>(R.id.llScanHistoryList)
        val emptyText = view?.findViewById<TextView>(R.id.tvEmptyScanHistory)
        val pastScans = dbHelper.getAllFacialScans(userId)

        if (pastScans.isNotEmpty()) {
            emptyText?.visibility = View.GONE
            for (scan in pastScans) {
                val resultView = TextView(requireContext()).apply {
                    val statusText = if (scan["detected"] == true) "⚠️ Droop Detected" else "✅ Normal"
                    text = "• ${scan["timestamp"]}: $statusText"
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                }
                historyLayout?.addView(resultView)
            }
        }
    }

    private fun loadAppointments(userId: Long) {
        val appointmentsLayout = view?.findViewById<LinearLayout>(R.id.llAppointmentsList)
        val emptyText = view?.findViewById<TextView>(R.id.tvEmptyAppointments)
        val pastAppointments = dbHelper.getAppointments(userId)

        if (pastAppointments.isNotEmpty()) {
            emptyText?.visibility = View.GONE
            for (apt in pastAppointments) {
                val resultView = TextView(requireContext()).apply {
                    val date = apt["apt_date"]
                    val time = apt["apt_time"]
                    val doctor = apt["doctor_name"]
                    text = "• $date at $time\n   $doctor"
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                }
                appointmentsLayout?.addView(resultView)
            }
        }
    }
}