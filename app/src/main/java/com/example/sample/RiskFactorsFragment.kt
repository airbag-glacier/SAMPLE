package com.example.sample

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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RiskFactorsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private val appThemeColor = "#D81B60".toColorInt()

    // Specific text options expected by the Kaggle model
    private val genderOptions = arrayOf("Male", "Female", "Other")
    private val workOptions = arrayOf("Private", "Self-employed", "Government", "Student", "Never worked")
    private val smokingOptions = arrayOf("Formerly smoked", "Never smoked", "Smokes")

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

        // Navigation
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            // Directly navigate to the scanner
            findNavController().navigate(R.id.action_global_scan)
        }
        view.findViewById<ImageView>(R.id.btnHome)?.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
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

            if (isSaved) {
                syncToDoctorDatabase(answers) // Send full Kaggle data to Python server
            } else {
                Toast.makeText(requireContext(), "Database Error.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Error: User ID not found.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- SERVER SYNC & NAVIGATION ---
    private fun syncToDoctorDatabase(answers: Map<String, Any>) {
        val loadingDialog = showThemedLoadingDialog()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject()
                for ((key, value) in answers) {
                    jsonObject.put(key, value)
                }

                // REMEMBER: Verify your laptop's Wi-Fi IP address!
                val url = URL("http://192.168.1.15:5000/predict_risk")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    val input = jsonObject.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(response)

                    if (responseJson.getBoolean("success")) {
                        val percentage = (responseJson.getDouble("risk_score") * 100).toInt()

                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()

                            // Navigate directly to the new Assessment Result Fragment!
                            val bundle = Bundle().apply {
                                putInt("RISK_PERCENTAGE", percentage)
                            }
                            findNavController().navigate(R.id.action_riskFactors_to_assessmentResult, bundle)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Error: ${responseJson.getString("error")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Failed to connect to server.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Network Error: Is the Python server running?", Toast.LENGTH_LONG).show()
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
            addView(TextView(requireContext()).apply { text = "Syncing with doctor's database..."; textSize = 16f; setPadding(30, 0, 0, 0); setTextColor(Color.BLACK) })
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(layout).setCancelable(false).show()
    }
}