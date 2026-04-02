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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        if (userId != -1L) {

            // CHECK PATH: Did we just finish an assessment, or did we open from the menu?
            if (arguments != null && requireArguments().containsKey("RISK_PERCENTAGE")) {

                // PATH 1: NEW ASSESSMENT (Arguments exist)
                val newRiskPercentage = requireArguments().getInt("RISK_PERCENTAGE")

                view.findViewById<TextView>(R.id.tvRiskPercentage).text = "$newRiskPercentage%"
                updateClinicalRiskUI(view, newRiskPercentage)

                // Save to DB and show the alert
                saveAssessmentToDatabase(dbHelper, userId, newRiskPercentage)
                showRiskDialog(newRiskPercentage, userId)

                // Clear the argument so if they rotate their phone, it doesn't save a duplicate!
                requireArguments().remove("RISK_PERCENTAGE")

            } else {

                // PATH 2: HISTORY MODE
                val latestAssessment = dbHelper.getLatestRiskAssessment(userId)

                if (latestAssessment != null) {
                    // Pull the last saved score from the database
                    val savedRisk = latestAssessment["lr_prediction"] as? Double ?: 0.0
                    val riskInt = savedRisk.toInt()

                    view.findViewById<TextView>(R.id.tvRiskPercentage).text = "$riskInt%"
                    updateClinicalRiskUI(view, riskInt)
                } else {
                    // They have never taken an assessment
                    view.findViewById<TextView>(R.id.tvRiskPercentage).text = "0%"
                    view.findViewById<TextView>(R.id.tvClinicalRiskDetails).text = "No assessment history found. Please update your risk factors."
                }

                showIncompleteDataWarning(dbHelper, userId)
            }

            // Always fetch and display the YOLOv10 Facial Scan regardless of the path
            updateYoloStatusUI(view, dbHelper, userId)

        } else {
            Toast.makeText(requireContext(), "Error: User Session Not Found", Toast.LENGTH_SHORT).show()
        }

        // NAVIGATION TO HOME SCREEN
        view.findViewById<MaterialButton>(R.id.btnReturnHome).setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    private fun updateClinicalRiskUI(view: View, riskPercentage: Int) {

        val tvClinicalDetails = view.findViewById<TextView>(R.id.tvClinicalRiskDetails)

        if (tvClinicalDetails != null) {
            val riskLevel = when {
                riskPercentage < 30 -> "Low"
                riskPercentage < 60 -> "Moderate"
                else -> "High"
            }

            tvClinicalDetails.text = "Based on your clinical health factors, our Logistic Regression model calculated a $riskPercentage% probability of stroke ($riskLevel Risk)."
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

    private fun showRiskDialog(riskPercentage: Int, userId: Long) {
        // Calculate the severity
        val riskLevel = when {
            riskPercentage < 30 -> "Low"
            riskPercentage < 60 -> "Moderate"
            else -> "High"
        }

        // Set a dynamic message based on the risk
        val recommendationMessage = when (riskLevel) {
            "High" -> "CRITICAL: Please consult a doctor immediately and proceed to the nearest hospital."
            "Moderate" -> "WARNING: We recommend scheduling a check-up with your doctor soon."
            else -> "Good news! Keep maintaining a healthy lifestyle."
        }

        // Build and display the pop-up
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("DeTechStroke Assessment Result")
            .setMessage("Risk Level: $riskLevel ($riskPercentage%)\n\n$recommendationMessage")
            .setPositiveButton("Sync to Cloud") { dialog, _ ->
                // Trigger the cloud sync ONLY when they click this button
                Toast.makeText(requireContext(), "Syncing to Doctor's Dashboard...", Toast.LENGTH_SHORT).show()
                CloudSyncManager(requireContext()).syncLocalDatabaseToCloud(userId)
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false) // Prevents them from clicking outside to close it
            .show()
    }

    private fun showIncompleteDataWarning(dbHelper: DatabaseHelper, userId: Long) {
        val latestAssessment = dbHelper.getLatestRiskAssessment(userId)
        val latestScan = dbHelper.getLatestFacialScan(userId)

        val missingRisk = latestAssessment == null
        val missingScan = latestScan == null

        // If neither is missing, we just do nothing and let them see their results!
        if (!missingRisk && !missingScan) return

        val title = "Incomplete Assessment"
        val message = when {
            missingRisk && missingScan -> "You haven't completed your Risk Factor Questionnaire or Facial Scan yet.\n\nPlease complete both to get your comprehensive stroke risk assessment."
            missingRisk -> "You haven't completed your Risk Factor Questionnaire.\n\nPlease complete it to calculate your clinical risk score."
            else -> "You haven't completed your Facial Scan.\n\nPlease use the camera tab to check for facial asymmetry and finalize your assessment."
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }
}