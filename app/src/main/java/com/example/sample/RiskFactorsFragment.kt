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

    // Kaggle Specific String Options
    private val genderOptions = arrayOf("Male", "Female", "Other")
    private val workOptions = arrayOf("Private", "Self-employed", "Govt_job", "children", "Never_worked")
    private val smokingOptions = arrayOf("formerly smoked", "never smoked", "smokes", "Unknown")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_risk_factors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        // --- Setup Spinners (Dropdowns) ---
        view.findViewById<Spinner>(R.id.spinGender).adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genderOptions)
        view.findViewById<Spinner>(R.id.spinWork).adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, workOptions)
        view.findViewById<Spinner>(R.id.spinSmoking).adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, smokingOptions)

        // --- Setup Buttons ---
        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { findNavController().popBackStack() }
        view.findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener { submitForm(view) }

        view.findViewById<FloatingActionButton>(R.id.btnCamera)?.setOnClickListener { findNavController().navigate(R.id.action_home_to_scan) }
        view.findViewById<ImageView>(R.id.btnHome)?.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
    }

    private fun getRadioString(view: View, groupId: Int): String? {
        val radioGroup = view.findViewById<RadioGroup>(groupId)
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId == -1) return null
        return view.findViewById<RadioButton>(selectedId).text.toString()
    }

    private fun getRadioInt(view: View, groupId: Int): Int {
        return if (getRadioString(view, groupId) == "Yes") 1 else 0
    }

    private fun submitForm(view: View) {
        val ageText = view.findViewById<EditText>(R.id.etAge).text.toString()
        val glucoseText = view.findViewById<EditText>(R.id.etGlucose).text.toString()
        val bmiText = view.findViewById<EditText>(R.id.etBmi).text.toString()

        val hypertension = getRadioString(view, R.id.rgHypertension)
        val heartDisease = getRadioString(view, R.id.rgHeart)
        val everMarried = getRadioString(view, R.id.rgMarried)
        val residence = getRadioString(view, R.id.rgResidence)

        // Validation: Prevent submitting empty fields
        if (ageText.isEmpty() || glucoseText.isEmpty() || bmiText.isEmpty() ||
            hypertension == null || heartDisease == null || everMarried == null || residence == null) {
            Toast.makeText(requireContext(), "Please answer all questions before submitting.", Toast.LENGTH_LONG).show()
            return
        }

        // Map data perfectly to the Kaggle columns using 'Any' to hold both Strings and Doubles
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
            "smoking_status" to view.findViewById<Spinner>(R.id.spinSmoking).selectedItem.toString()
        )

        // Save Locally and Sync
        if (dbHelper.insertRiskFactors(answers)) {
            syncToDoctorDatabase(answers)
        } else {
            Toast.makeText(requireContext(), "Database Error. Could not save locally.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncToDoctorDatabase(answers: Map<String, Any>) {
        val loadingDialog = showThemedLoadingDialog()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert Map to JSON
                val jsonObject = JSONObject()
                for ((key, value) in answers) {
                    jsonObject.put(key, value)
                }

                // Change IP back to your laptop IP!
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
                            showThemedResultDialog(percentage)
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
            } catch (_: Exception) {
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

    private fun showThemedResultDialog(riskPercentage: Int) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Assessment Complete")
            .setMessage("Based on your risk factors, your stroke probability is $riskPercentage%.\n\nThis data has been successfully securely synced to your doctor's Admin Database.")
            .setPositiveButton("Back to Home") { _, _ -> findNavController().popBackStack(R.id.homeFragment, false) }
            .setCancelable(false)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(appThemeColor)
    }
}