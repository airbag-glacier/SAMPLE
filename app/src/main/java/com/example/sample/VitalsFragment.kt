package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.pow

class VitalsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vitals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back Button Logic
        val btnBackVitals = view.findViewById<ImageView>(R.id.btnBackVitals)
        btnBackVitals?.setOnClickListener {
            findNavController().popBackStack()
        }
        //  Find all the Dropdown UI Elements
        val dropdownSystolic = view.findViewById<AutoCompleteTextView>(R.id.dropdownSystolic)
        val dropdownDiastolic = view.findViewById<AutoCompleteTextView>(R.id.dropdownDiastolic)
        val dropdownHeartRate = view.findViewById<AutoCompleteTextView>(R.id.dropdownHeartRate)
        val dropdownOxygen = view.findViewById<AutoCompleteTextView>(R.id.dropdownOxygen)
        val dropdownHeight = view.findViewById<AutoCompleteTextView>(R.id.dropdownHeight)
        val dropdownWeight = view.findViewById<AutoCompleteTextView>(R.id.dropdownWeight)

        // 2. Generate the Number Ranges for the Dropdowns
        val systolicRange = (70..250 step 1).map { it.toString() }
        val diastolicRange = (40..150 step 1).map { it.toString() }
        val hrRange = (40..200 step 1).map { it.toString() }
        val oxygenRange = (70..100 step 1).map { it.toString() }
        val heightRange = (100..250 step 1).map { it.toString() }
        val weightRange = (30..200 step 1).map { it.toString() }

        // 3. Attach the Data to the Dropdowns using ArrayAdapters
        dropdownSystolic.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, systolicRange))
        dropdownDiastolic.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, diastolicRange))
        dropdownHeartRate.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hrRange))
        dropdownOxygen.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, oxygenRange))
        dropdownHeight.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, heightRange))
        dropdownWeight.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, weightRange))


        // Save Button Logic
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveVitals)
        btnSave.setOnClickListener {
            val heightCm = dropdownHeight.text.toString().toDoubleOrNull() ?: 0.0
            val weightKg = dropdownWeight.text.toString().toDoubleOrNull() ?: 0.0

            // ERD Mapping: The ERD only has BMI. So we calculate it here.
            var bmi = 0.0
            if (heightCm > 0) {
                val heightMeters = heightCm / 100
                bmi = weightKg / (heightMeters.pow(2))
            }

            val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)
            val dbHelper = DatabaseHelper(requireContext())

            if (userId != -1L) {
                // Save ONLY the BMI to the ERD's Health Profile
                val isSaved = dbHelper.updateVitalsToERD(userId, bmi)

                if (isSaved) {
                    Toast.makeText(requireContext(), "Vitals Saved to Profile!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to save vitals.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Bottom Navigation Logic
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_global_scan)
        }

        val btnHome = view.findViewById<ImageView>(R.id.btnHome)
        btnHome?.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        view.findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("DeTechStroke")
                .setMessage("Developers:\nGabriel Garcia\nPhoebe Andrei Quan\nNatsuki Ushijima\n\n© 2026 All Rights Reserved.")
                .setPositiveButton("Restart App") { _, _ ->
                    val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}