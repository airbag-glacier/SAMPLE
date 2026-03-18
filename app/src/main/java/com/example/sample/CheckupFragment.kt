package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CalendarView
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CheckupFragment : Fragment() {

    // SAMPLE LANG HEHE: List of Philippine Doctors
    private val doctorsList = mutableListOf(
        "Dr. Jose Rizal (Ophthalmology)",
        "Dr. Fe Del Mundo (Pediatrics)",
        "Dr. Vicki Belo (Dermatology)",
        "Dr. Willie Ong (Cardiology)",
        "Dr. Juan Dela Cruz (General Practice)"
    )


    private val fullyBookedDays = listOf(15, 30)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate
        return inflater.inflate(R.layout.fragment_checkup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val dropdownDoctor = view.findViewById<AutoCompleteTextView>(R.id.dropdownDoctor)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val tvDateStatus = view.findViewById<TextView>(R.id.tvDateStatus)
        val radioGroupTime = view.findViewById<RadioGroup>(R.id.radioGroupTime)
        val btnAddDoctor = view.findViewById<MaterialButton>(R.id.btnAddDoctor)

        setupDropdown(dropdownDoctor)

        // LOCKING controls initially
        calendarView.isEnabled = false

        toggleTimeSelection(radioGroupTime, false)

        //  Doctor is Selected
        dropdownDoctor.setOnItemClickListener { _, _, position, _ ->
            val selectedDoctor = doctorsList[position]

            calendarView.isEnabled = true


            tvDateStatus.text = "Checking schedule for $selectedDoctor..."
            tvDateStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))

            Toast.makeText(requireContext(), "Schedule loaded for $selectedDoctor", Toast.LENGTH_SHORT).show()
        }

        //  Date is Clicked
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Note: 'month' is 0-indexed (0 = Jan, 11 = Dec)

            // Check if the selected day is in our "Fully Booked" list
            if (fullyBookedDays.contains(dayOfMonth)) {
                // CASE: FULL
                tvDateStatus.text = "Sorry, fully booked on ${month + 1}/$dayOfMonth/$year."
                tvDateStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))

                toggleTimeSelection(radioGroupTime, false) // Disable times
            } else {
                // CASE: AVAILABLE
                tvDateStatus.text = "Date Available! Please select a time."
                tvDateStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

                toggleTimeSelection(radioGroupTime, true) // Enable times
            }
        }

        // --- LISTENER: Add New Doctor Button ---
        btnAddDoctor.setOnClickListener {
            // Create a dialog builder
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Add New Doctor")

            // Set up the input field programmatically
            val input = EditText(requireContext())
            input.hint = "e.g., Dr. Jane Doe (Neurology)"
            // Add padding so the text isn't flush against the dialog edges
            input.setPadding(50, 40, 50, 40)
            builder.setView(input)

            // Set up the "Add" button inside the dialog
            builder.setPositiveButton("Add") { dialog, _ ->
                val newDocName = input.text.toString().trim()

                if (newDocName.isNotEmpty()) {
                    // Add the user's string to the list
                    doctorsList.add(newDocName)

                    // Refresh the dropdown so it shows up immediately
                    setupDropdown(dropdownDoctor)

                    Toast.makeText(requireContext(), "Added $newDocName!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Doctor name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }

            // Set up the "Cancel" button
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            // Show the dialog
            builder.show()
        }
    }

    // Helper function to setup the dropdown adapter
    private fun setupDropdown(dropdown: AutoCompleteTextView) {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, doctorsList)
        dropdown.setAdapter(adapter)
    }

    // Helper function to enable/disable radio buttons
    private fun toggleTimeSelection(group: RadioGroup, isEnabled: Boolean) {
        for (i in 0 until group.childCount) {
            group.getChildAt(i).isEnabled = isEnabled
        }
        if (!isEnabled) {
            group.clearCheck()
        }
    }
}