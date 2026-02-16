package com.example.sample // CHECK: Ensure this matches your package name!

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CalendarView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class CheckupFragment : Fragment() {

    // SAMPLE LANG HEHE: List of Philippine Doctors ---
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

        // INIT VALS
        val dropdownDoctor = view.findViewById<AutoCompleteTextView>(R.id.dropdownDoctor)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val tvDateStatus = view.findViewById<TextView>(R.id.tvDateStatus)
        val radioGroupTime = view.findViewById<RadioGroup>(R.id.radioGroupTime)
        val btnAddDoctor = view.findViewById<MaterialButton>(R.id.btnAddDoctor)

        //  Dropdown
        setupDropdown(dropdownDoctor)

        // LOCKING controls initially
        calendarView.isEnabled = false
        // Note: CalendarView doesn't visually "disable" well, so we rely on the listener logic below
        toggleTimeSelection(radioGroupTime, false)

        //  Doctor is Selected ---
        dropdownDoctor.setOnItemClickListener { _, _, position, _ ->
            val selectedDoctor = doctorsList[position]

            // Enable the calendar
            calendarView.isEnabled = true

            // Update UI feedback
            tvDateStatus.text = "Checking schedule for $selectedDoctor..."
            tvDateStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))

            Toast.makeText(requireContext(), "Schedule loaded for $selectedDoctor", Toast.LENGTH_SHORT).show()
        }

        //  Date is Clicked ---
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
            // For prototype: Add a generic "New Doctor" to the list
            val newDocName = "Dr. New Entry ${doctorsList.size + 1} (General)"
            doctorsList.add(newDocName)

            // Refresh the dropdown list so the new name appears
            setupDropdown(dropdownDoctor)

            Toast.makeText(requireContext(), "Added $newDocName to database!", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to setup the dropdown adapter
    private fun setupDropdown(dropdown: AutoCompleteTextView) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, doctorsList)
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