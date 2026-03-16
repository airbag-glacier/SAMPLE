package com.example.sample

import com.example.sample.R
import androidx.core.graphics.toColorInt
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_risk_factors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Setup Back Button ---
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        dbHelper = DatabaseHelper(requireContext())

        // --- STUDY-SPECIFIC RISK FACTOR QUESTIONS ---
        setupQuestion(view, R.id.qHypertension, "1. Have you ever been diagnosed with or treated for Hypertension (High Blood Pressure)?")
        setupQuestion(view, R.id.qDiabetes, "2. Have you ever been diagnosed with or treated for Diabetes Mellitus?")
        setupQuestion(view, R.id.qSmoking, "3. Do you currently smoke tobacco, or do you have a history of regular smoking?")
        setupQuestion(view, R.id.qDyslipidemia, "4. Have you been diagnosed with Dyslipidemia (abnormal or high cholesterol)?")
        setupQuestion(view, R.id.qObesity, "5. Have you been formally diagnosed with Obesity, or is your Body Mass Index (BMI) 30 or higher?")
        setupQuestion(view, R.id.qPriorStroke, "6. Have you ever suffered a Prior Stroke or Transient Ischemic Attack (TIA / mini-stroke)?")
        setupQuestion(view, R.id.qAtrialFib, "7. Have you been diagnosed with Atrial Fibrillation (an irregular, often rapid heart rate)?")
        setupQuestion(view, R.id.qSedentary, "8. Would you describe your lifestyle as sedentary (engaging in less than 150 minutes of moderate exercise per week)?")
        setupQuestion(view, R.id.qAlcohol, "9. Do you engage in heavy or regular alcohol consumption (more than 1-2 standard drinks per day)?")
        setupQuestion(view, R.id.qKidneyDisease, "10. Have you ever been diagnosed with Chronic Kidney Disease (CKD)?")

        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmit)
        btnSubmit.setOnClickListener {
            submitForm(view)
        }

        // --- Bottom Navigation Setup ---
        view.findViewById<FloatingActionButton>(R.id.btnCamera)?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }
        view.findViewById<ImageView>(R.id.btnHome)?.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    private fun setupQuestion(parentView: View, includeId: Int, questionText: String) {
        val questionLayout = parentView.findViewById<View>(includeId)
        val tvQuestion = questionLayout.findViewById<TextView>(R.id.tvQuestion)
        tvQuestion.text = questionText
    }

    // Returns an Integer (1, 0, or -1 for unanswered)
    private fun getAnswerAsInt(parentView: View, includeId: Int): Int {
        val questionLayout = parentView.findViewById<View>(includeId)
        val radioGroup = questionLayout.findViewById<RadioGroup>(R.id.radioGroup)

        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId == -1) return -1 // -1 means BLANK

        val radioButton = questionLayout.findViewById<RadioButton>(selectedId)
        return if (radioButton.text.toString() == "Yes") 1 else 0
    }

    private fun submitForm(view: View) {
        // Gather all answers as 1s and 0s
        val answers = mapOf(
            "hypertension" to getAnswerAsInt(view, R.id.qHypertension),
            "diabetes" to getAnswerAsInt(view, R.id.qDiabetes),
            "smoking" to getAnswerAsInt(view, R.id.qSmoking),
            "dyslipidemia" to getAnswerAsInt(view, R.id.qDyslipidemia),
            "obesity" to getAnswerAsInt(view, R.id.qObesity),
            "prior_stroke" to getAnswerAsInt(view, R.id.qPriorStroke),
            "atrial_fibrillation" to getAnswerAsInt(view, R.id.qAtrialFib),
            "sedentary" to getAnswerAsInt(view, R.id.qSedentary),
            "alcohol" to getAnswerAsInt(view, R.id.qAlcohol),
            "ckd" to getAnswerAsInt(view, R.id.qKidneyDisease)
        )

        // Prevent submission if ANY question was skipped (value is -1)
        if (answers.containsValue(-1)) {
            Toast.makeText(requireContext(), "Please answer all questions before submitting.", Toast.LENGTH_LONG).show()
            return
        }

        // Save Locally in SQLite
        val isSavedLocally = dbHelper.insertRiskFactors(answers)

        if (isSavedLocally) {
            // Initiate the Cloud Upload to Python Server
            syncToDoctorDatabase(answers)
        } else {
            Toast.makeText(requireContext(), "Database Error. Could not save locally.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncToDoctorDatabase(answers: Map<String, Int>) {
        val loadingDialog = showThemedLoadingDialog()

        // Launch a background thread for the network request
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Convert our SQLite Map to a JSON Object
                val jsonObject = JSONObject()
                for ((key, value) in answers) {
                    jsonObject.put(key, value)
                }
                val jsonString = jsonObject.toString()

                // 2. Connect to Python Server (CHANGE THIS IP ADDRESS TO YOUR LAPTOP'S IP!)
                val url = URL("http://192.168.1.15:5000/predict_risk")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                // 3. Send the JSON Data
                connection.outputStream.use { os ->
                    val input = jsonString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // 4. Read the Python Response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(response)

                    if (responseJson.getBoolean("success")) {
                        // Extract the score computed by Logistic Regression
                        val riskScore = responseJson.getDouble("risk_score")
                        val percentage = (riskScore * 100).toInt()

                        // Switch back to Main Thread to update the UI
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            showThemedResultDialog(percentage)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Server Error: ${responseJson.getString("error")}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), "Network Error: Make sure your Python server is running.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- UI: Themed Loading Dialog ---
    private fun showThemedLoadingDialog(): AlertDialog {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(50, 50, 50, 50)
            gravity = android.view.Gravity.CENTER_VERTICAL

            val progressBar = ProgressBar(requireContext()).apply {
                indeterminateTintList = ColorStateList.valueOf(appThemeColor)
            }
            addView(progressBar)

            val text = TextView(requireContext()).apply {
                text = "Syncing with doctor's database..."
                textSize = 16f
                setPadding(30, 0, 0, 0)
                setTextColor(Color.BLACK)
            }
            addView(text)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(layout)
            .setCancelable(false)
            .show()
    }

    // --- UI: Themed Result Dialog ---
    private fun showThemedResultDialog(riskPercentage: Int) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Assessment Complete")
            .setMessage("Based on your risk factors, your stroke probability is $riskPercentage%.\n\nThis data has been successfully securely synced to your doctor's Admin Database.")
            .setPositiveButton("Back to Home") { _, _ ->
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            .setCancelable(false)
            .show()

        // Color the 'Back to Home' button Pink to match the theme
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(appThemeColor)
    }
}