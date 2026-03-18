package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssessmentResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assessment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)
        val riskPercentage = arguments?.getInt("RISK_PERCENTAGE") ?: 0

        view.findViewById<TextView>(R.id.tvRiskPercentage).text = "$riskPercentage%"

        if (userId != -1L) {
            // 1. Save the Logistic Regression Result to the Database
            saveAssessmentToDatabase(dbHelper, userId, riskPercentage)

            // 2. Fetch and Display the latest YOLOv10 Facial Scan
            updateYoloStatusUI(view, dbHelper, userId)
        } else {
            Toast.makeText(requireContext(), "Error: User Session Not Found", Toast.LENGTH_SHORT).show()
        }
//NAVIGATION TO HOME SCREEN
        view.findViewById<MaterialButton>(R.id.btnReturnHome).setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    private fun saveAssessmentToDatabase(dbHelper: DatabaseHelper, userId: Long, riskPercentage: Int) {
        // Categorize the risk level
        val riskLevel = when {
            riskPercentage < 30 -> "Low"
            riskPercentage < 60 -> "Moderate"
            else -> "High"
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val isSaved = dbHelper.insertRiskAssessment(
            userId = userId,
            lrPrediction = riskPercentage.toDouble(),
            riskLevel = riskLevel,
            timestamp = timestamp
        )

        if (!isSaved) {
            Toast.makeText(requireContext(), "Failed to log assessment in database.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateYoloStatusUI(view: View, dbHelper: DatabaseHelper, userId: Long) {
        val tvYoloStatus = view.findViewById<TextView>(R.id.tvYoloStatus)
        val latestScan = dbHelper.getLatestFacialScan(userId)

        if (latestScan != null) {
            val hasAsymmetry = latestScan["detected"] as Boolean
            val scanDate = latestScan["timestamp"] as String

            if (hasAsymmetry) {
                tvYoloStatus.text = "ALERT: Facial drooping/asymmetry was detected during your last scan on $scanDate."
                tvYoloStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            } else {
                tvYoloStatus.text = "Good news: No facial drooping detected in your latest scan ($scanDate)."
                tvYoloStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        } else {
            tvYoloStatus.text = "No facial scan data found. Please take a face scan using the camera tab to complete your assessment."
        }
    }
}