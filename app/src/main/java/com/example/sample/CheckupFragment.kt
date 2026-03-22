package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CheckupFragment : Fragment() {

    private val doctorsList = mutableListOf(
        "Dr. Jose Rizal (Ophthalmology)",
        "Dr. Fe Del Mundo (Pediatrics)",
        "Dr. Vicki Belo (Dermatology)",
        "Dr. Willie Ong (Cardiology)",
        "Dr. Juan Dela Cruz (General Practice)"
    )

    private val fullyBookedDays = listOf(15, 30)

    // Variables to hold the user's selections
    private var selectedDoctor: String = ""
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_checkup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dropdownDoctor = view.findViewById<AutoCompleteTextView>(R.id.dropdownDoctor)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val tvDateStatus = view.findViewById<TextView>(R.id.tvDateStatus)
        val radioGroupTime = view.findViewById<RadioGroup>(R.id.radioGroupTime)
        val btnAddDoctor = view.findViewById<MaterialButton>(R.id.btnAddDoctor)
        val btnSaveAppointment = view.findViewById<MaterialButton>(R.id.btnSaveAppointment)

        val dbHelper = DatabaseHelper(requireContext())
        val userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        setupDropdown(dropdownDoctor)

        calendarView.isEnabled = false
        toggleTimeSelection(radioGroupTime, false)

        // 1. Doctor Selection
        dropdownDoctor.setOnItemClickListener { _, _, position, _ ->
            selectedDoctor = doctorsList[position]
            calendarView.isEnabled = true
            tvDateStatus.text = "Checking schedule for $selectedDoctor..."
            tvDateStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        // 2. Date Selection
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            if (fullyBookedDays.contains(dayOfMonth)) {
                tvDateStatus.text = "Sorry, fully booked on ${month + 1}/$dayOfMonth/$year."
                tvDateStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                toggleTimeSelection(radioGroupTime, false)
                selectedDate = ""
            } else {
                tvDateStatus.text = "Date Available! Please select a time."
                tvDateStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                toggleTimeSelection(radioGroupTime, true)
                selectedDate = "${month + 1}/$dayOfMonth/$year"
            }
        }

        // 3. Time Selection
        radioGroupTime.setOnCheckedChangeListener { group, checkedId ->
            val radioButton = view.findViewById<RadioButton>(checkedId)
            selectedTime = radioButton?.text.toString()
        }

        // 4. SAVE APPOINTMENT LOGIC
        btnSaveAppointment.setOnClickListener {
            if (userId == -1L) {
                Toast.makeText(requireContext(), "Error: User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDoctor.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a doctor, date, and time.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save to Local SQLite
            val isSaved = dbHelper.insertAppointment(userId, selectedDoctor, selectedDate, selectedTime)

            if (isSaved) {
                Toast.makeText(requireContext(), "Appointment Saved!", Toast.LENGTH_SHORT).show()

                // TRIGGER CLOUD SYNC IMMEDIATELY
                CloudSyncManager(requireContext()).syncLocalDatabaseToCloud(userId)

                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to save appointment.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- LISTENER: Add New Doctor Button ---
        btnAddDoctor.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Add New Doctor")
            val input = EditText(requireContext())
            input.hint = "e.g., Dr. Jane Doe (Neurology)"
            input.setPadding(50, 40, 50, 40)
            builder.setView(input)

            builder.setPositiveButton("Add") { dialog, _ ->
                val newDocName = input.text.toString().trim()
                if (newDocName.isNotEmpty()) {
                    doctorsList.add(newDocName)
                    setupDropdown(dropdownDoctor)
                    Toast.makeText(requireContext(), "Added $newDocName!", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
        }

        // Bottom Nav Logic
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            // Directly navigate to the scanner
            findNavController().navigate(R.id.action_global_scan)
        }
        view.findViewById<ImageView>(R.id.btnHome)?.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
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

    private fun setupDropdown(dropdown: AutoCompleteTextView) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, doctorsList)
        dropdown.setAdapter(adapter)
    }

    private fun toggleTimeSelection(group: RadioGroup, isEnabled: Boolean) {
        for (i in 0 until group.childCount) {
            group.getChildAt(i).isEnabled = isEnabled
        }
        if (!isEnabled) {
            group.clearCheck()
            selectedTime = "" // Reset time if date becomes invalid
        }
    }
}