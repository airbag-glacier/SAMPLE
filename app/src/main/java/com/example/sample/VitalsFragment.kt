package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton

class VitalsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vitals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dropdownSystolic = view.findViewById<AutoCompleteTextView>(R.id.dropdownSystolic)
        val dropdownDiastolic = view.findViewById<AutoCompleteTextView>(R.id.dropdownDiastolic)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveVitals)

        // 1. Setup Blood Pressure Dropdowns
        val systolicRange = (70..220).toList().map { it.toString() }
        val systolicAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, systolicRange)
        dropdownSystolic.setAdapter(systolicAdapter)

        // Diastolic: 40 to 130
        val diastolicRange = (40..140).toList().map { it.toString() }
        val diastolicAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, diastolicRange)
        dropdownDiastolic.setAdapter(diastolicAdapter)


        btnSave.setOnClickListener {
            // Get values from the UI dropdowns
            val sys = dropdownSystolic.text.toString().toIntOrNull() ?: 0
            val dia = dropdownDiastolic.text.toString().toIntOrNull() ?: 0

            val dropdownHeight = view.findViewById<AutoCompleteTextView>(R.id.dropdownHeight)
            val dropdownWeight = view.findViewById<AutoCompleteTextView>(R.id.dropdownWeight)
            val height = dropdownHeight.text.toString().toIntOrNull() ?: 0
            val weight = dropdownWeight.text.toString().toIntOrNull() ?: 0

            // Save to Database
            val dbHelper = DatabaseHelper(requireContext())
            val isSaved = dbHelper.insertVitals(sys, dia, height, weight)

            if (isSaved) {
                Toast.makeText(requireContext(), "Vitals Saved Successfully!", Toast.LENGTH_SHORT).show()
                // Go back to the Home Screen automatically to see the updated header!
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to save vitals.", Toast.LENGTH_SHORT).show()
            }
        }

// NAVIGATION LOGIC
        val btnHome = view.findViewById<View>(R.id.btnHome)
        btnHome.setOnClickListener {
            // Go back to Home
            findNavController().popBackStack()
        }

        val btnCamera = view.findViewById<View>(R.id.btnCamera)
        btnCamera.setOnClickListener {
            // Go to Camera (Use the Fragment ID directly)
            findNavController().navigate(R.id.scanFragment)
        }
    }
}