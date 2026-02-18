package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
            // Here you would save to database. For now, we show a confirmation.
            Toast.makeText(requireContext(), "Vitals Saved Successfully!", Toast.LENGTH_SHORT).show()


        }
    }
}