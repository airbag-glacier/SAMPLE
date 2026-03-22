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

class BloodChemFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_blood_chem, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the Dropdown Inputs
        val etTotalCholesterol = view.findViewById<AutoCompleteTextView>(R.id.etTotalCholesterol)
        val etHdl = view.findViewById<AutoCompleteTextView>(R.id.etHdl)
        val etLdl = view.findViewById<AutoCompleteTextView>(R.id.etLdl)
        val etTriglycerides = view.findViewById<AutoCompleteTextView>(R.id.etTriglycerides)
        val etFbs = view.findViewById<AutoCompleteTextView>(R.id.etFbs)

        val btnSaveBloodChem = view.findViewById<MaterialButton>(R.id.btnSaveBloodChem)

        // GENERATE DROPDOWN DATA
        val cholesterolRange = (100..350 step 5).map { it.toString() }
        val hdlRange = (20..100 step 2).map { it.toString() }
        val ldlRange = (40..250 step 5).map { it.toString() }
        val triRange = (50..400 step 10).map { it.toString() }
        val fbsRange = (60..250 step 5).map { it.toString() }

        // Attach the data lists to the UI components
        etTotalCholesterol.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cholesterolRange))
        etHdl.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hdlRange))
        etLdl.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ldlRange))
        etTriglycerides.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, triRange))
        etFbs.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fbsRange))

        // Save Button Logic
        btnSaveBloodChem.setOnClickListener {
            val totalChol = etTotalCholesterol.text.toString().trim()
            val hdl = etHdl.text.toString().trim()
            val ldl = etLdl.text.toString().trim()
            val tri = etTriglycerides.text.toString().trim()
            val fbs = etFbs.text.toString().trim()

            // Basic validation
            if (totalChol.isEmpty() && hdl.isEmpty() && ldl.isEmpty() && tri.isEmpty() && fbs.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one value to save.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //SAVE TO SQLITE DATABASE
            val dbHelper = DatabaseHelper(requireContext())
            val userId = requireActivity().intent?.getLongExtra("USER_ID", -1L) ?: -1L

            if (userId != -1L) {
                // ERD only tracks Cholesterol right now, so we pass the totalChol
                val isSaved = dbHelper.updateBloodChemToERD(userId, totalChol.toDoubleOrNull() ?: 0.0)

                if (isSaved) {
                    Toast.makeText(requireContext(), "Blood Chemistry Records Saved!", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Database Error. Could not save.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ==========================================
        // Bottom Navigation Setup
        // ==========================================
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            // Directly navigate to the scanner
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