package com.example.sample

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RiskFactorsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private val appThemeColor = "#D81B60".toColorInt()

    // Specific text options expected by the Kaggle model
    private val genderOptions = arrayOf("Male", "Female", "Other")
    private val workOptions = arrayOf("Private", "Self-employed", "Government", "Student", "Never worked")
    private val smokingOptions = arrayOf("Formerly smoked", "Never smoked", "Smokes", "Unknown") // Added Unknown to match Kaggle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_risk_factors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        // Setup Spinners
        view.findViewById<Spinner>(R.id.spinGender).adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genderOptions)
        view.findViewById<Spinner>(R.id.spinWork).adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, workOptions)
        view.findViewById<Spinner>(R.id.spinSmoking).adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, smokingOptions)

        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { findNavController().popBackStack() }
        view.findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener { submitForm(view) }

        // ==========================================
        // Bottom Navigation Setup
        // ==========================================
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_global_scan)
        }

        val btnHome = view.findViewById<ImageView>(R.id.btnHome)
        btnHome?.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

// Bottom Navigation: Upgraded List Menu Dialog
        view.findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            val menuOptions = arrayOf("Assessment Result", "Emergency Contacts", "About / Credits", "Restart App")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("DeTechStroke Menu")
                .setItems(menuOptions) { _, which ->
                    when (which) {
                        0 -> findNavController().navigate(R.id.action_global_assessmentResult)

                        1 -> findNavController().navigate(R.id.action_global_emergencyContacts)

                        2 -> {
                            // Show the developer credits in a secondary pop-up
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("DeTechStroke")
                                .setMessage("Developers:\nGabriel Garcia\nPhoebe Andrei Quan\nNatsuki Ushijima\n\n© 2026 All Rights Reserved.")
                                .setPositiveButton("Close", null)
                                .show()
                        }

                        3 -> {
                            // Restart App Logic
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
    }

    // HELPER FUNCTIONS
    private fun getRadioString(view: View, groupId: Int): String? {
        val radioGroup = view.findViewById<RadioGroup>(groupId)
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId == -1) return null
        return view.findViewById<RadioButton>(selectedId).text.toString()
    }

    private fun getRadioInt(view: View, groupId: Int): Int {
        return if (getRadioString(view, groupId) == "Yes") 1 else 0
    }

    // --- MAIN SUBMISSION LOGIC ---
    private fun submitForm(view: View) {
        val ageText = view.findViewById<EditText>(R.id.etAge).text.toString()
        val glucoseText = view.findViewById<EditText>(R.id.etGlucose).text.toString()
        val bmiText = view.findViewById<EditText>(R.id.etBmi).text.toString()

        val hypertensionStr = getRadioString(view, R.id.rgHypertension)
        val heartDiseaseStr = getRadioString(view, R.id.rgHeart)
        val everMarried = getRadioString(view, R.id.rgMarried)
        val residence = getRadioString(view, R.id.rgResidence)
        val smokingStatus = view.findViewById<Spinner>(R.id.spinSmoking).selectedItem.toString()

        if (ageText.isEmpty() || glucoseText.isEmpty() || bmiText.isEmpty() ||
            hypertensionStr == null || heartDiseaseStr == null || everMarried == null || residence == null) {
            Toast.makeText(requireContext(), "Please answer all questions before submitting.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Pack data exactly as Kaggle/Python expects
        val answers = mapOf<String, Any>(
            "gender" to view.findViewById<Spinner>(R.id.spinGender).selectedItem.toString(),
            "age" to ageText.toDouble(),
            "hypertension" to getRadioInt(view, R.id.rgHypertension),
            "heart_disease" to getRadioInt(view, R.id.rgHeart),
            "ever_married" to everMarried,
            "work_type" to view.findViewById<Spinner>(R.id.spinWork).selectedItem.toString(),
            "Residence_type" to residence,
            "avg_glucose_level" to glucoseText.toDouble(),
            "bmi" to bmiText.toDouble(),
            "smoking_status" to smokingStatus
        )

        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        if (userId != -1L) {
            // 2. Map data to the ERD HealthRiskFactorProfile table constraints
            val isSmoker = if (smokingStatus.contains("smokes", ignoreCase = true)) 1 else 0
            val isDiabetic = if (glucoseText.toDouble() >= 126.0) 1 else 0 // Basic medical mapping

            val isSaved = dbHelper.updateRiskFactorsToERD(
                userId = userId,
                age = ageText.toInt(), // ERD stores age in User table
                hypertension = getRadioInt(view, R.id.rgHypertension),
                cardiacDisease = getRadioInt(view, R.id.rgHeart),
                bmi = bmiText.toDouble(),
                smoker = isSmoker,
                diabetes = isDiabetic
            )

            // THE CRITICAL FIX IS HERE
            if (isSaved) {
                runOfflineRiskAssessment(answers) // Run local TFLite model and background sync
            } else {
                Toast.makeText(requireContext(), "Database Error.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Error: User ID not found.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- OFFLINE AI INFERENCE & CLOUD SYNC ---
    private fun runOfflineRiskAssessment(answers: Map<String, Any>) {
        val loadingDialog = showThemedLoadingDialog()
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. KOTLIN DUMMY ENCODING (Replicating Pandas pd.get_dummies)
                // Note: The array size (e.g., 21) must perfectly match the number of columns in your Kaggle dataset
                val encodedFeatures = FloatArray(21)

                // Base Numerical Features
                encodedFeatures[0] = (answers["age"] as Double).toFloat()
                encodedFeatures[1] = (answers["hypertension"] as Int).toFloat()
                encodedFeatures[2] = (answers["heart_disease"] as Int).toFloat()
                encodedFeatures[3] = (answers["avg_glucose_level"] as Double).toFloat()
                encodedFeatures[4] = (answers["bmi"] as Double).toFloat()

                // Categorical Features (One-Hot Encoding)
                val gender = answers["gender"] as String
                if (gender == "Female") encodedFeatures[5] = 1f
                else if (gender == "Male") encodedFeatures[6] = 1f
                else encodedFeatures[7] = 1f // Other

                val everMarried = answers["ever_married"] as String
                if (everMarried == "No") encodedFeatures[8] = 1f
                else encodedFeatures[9] = 1f // Yes

                val workType = answers["work_type"] as String
                when (workType) {
                    "Government" -> encodedFeatures[10] = 1f
                    "Never worked" -> encodedFeatures[11] = 1f
                    "Private" -> encodedFeatures[12] = 1f
                    "Self-employed" -> encodedFeatures[13] = 1f
                    "Student" -> encodedFeatures[14] = 1f
                }

                val residence = answers["Residence_type"] as String
                if (residence == "Rural") encodedFeatures[15] = 1f
                else encodedFeatures[16] = 1f // Urban

                val smoke = answers["smoking_status"] as String
                when (smoke) {
                    "Unknown" -> encodedFeatures[17] = 1f
                    "Formerly smoked" -> encodedFeatures[18] = 1f
                    "Never smoked" -> encodedFeatures[19] = 1f
                    "Smokes" -> encodedFeatures[20] = 1f
                }

                // 2. RUN TENSORFLOW LITE INFERENCE
                val riskDetector = ClinicalRiskDetector(requireContext())
                val riskProbability = riskDetector.predictRisk(encodedFeatures)
                riskDetector.close() // Prevent memory leaks

                val percentage = (riskProbability * 100).toInt()


                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    // Navigate to Results Screen
                    val bundle = Bundle().apply {
                        putInt("RISK_PERCENTAGE", percentage)
                    }
                    findNavController().navigate(R.id.action_riskFactors_to_assessmentResult, bundle)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Offline AI Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showThemedLoadingDialog(): AlertDialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(ProgressBar(requireContext()).apply { indeterminateTintList = ColorStateList.valueOf(appThemeColor) })
            addView(TextView(requireContext()).apply { text = "Analyzing Risk Factors locally..."; textSize = 16f; setPadding(30, 0, 0, 0); setTextColor(Color.BLACK) })
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(layout).setCancelable(false).show()
    }
}