package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class EmergencyContactsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private var userId: Long = -1L

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private var contactsList = mutableListOf<Map<String, String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_emergency_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        userId = requireActivity().intent.getLongExtra("USER_ID", -1L)

        // UI References
        val btnOpenMap = view.findViewById<MaterialButton>(R.id.btnOpenHospitalMap)
        val etName = view.findViewById<TextInputEditText>(R.id.etContactName)
        val etRelation = view.findViewById<TextInputEditText>(R.id.etRelationship)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etPhone)
        val switchPrimary = view.findViewById<SwitchMaterial>(R.id.switchPrimary)
        val btnAddContact = view.findViewById<MaterialButton>(R.id.btnAddContact)

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewContacts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        contactsAdapter = ContactsAdapter(contactsList) { contactId ->
            deleteContact(contactId)
        }
        recyclerView.adapter = contactsAdapter

        if (userId != -1L) {
            loadContacts()
        } else {
            Toast.makeText(requireContext(), "Error: User session not found.", Toast.LENGTH_SHORT).show()
        }


        btnOpenMap.setOnClickListener {
            // Assumes you have an action defined in your nav_graph.xml
            findNavController().navigate(R.id.action_emergencyContacts_to_hospitalMap)
        }

        //
        btnAddContact.setOnClickListener {
            val name = etName.text.toString().trim()
            val relation = etRelation.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val isPrimary = if (switchPrimary.isChecked) 1 else 0

            if (name.isNotEmpty() && phone.isNotEmpty() && userId != -1L) {
                val isSaved = dbHelper.insertEmergencyContact(userId, name, relation, isPrimary, phone)

                if (isSaved) {
                    Toast.makeText(requireContext(), "Contact Saved!", Toast.LENGTH_SHORT).show()
                    // Clear inputs
                    etName.text?.clear()
                    etRelation.text?.clear()
                    etPhone.text?.clear()
                    switchPrimary.isChecked = false

                    loadContacts() // Refresh UI
                } else {
                    Toast.makeText(requireContext(), "Database Error.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Name and Phone are required.", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
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

    private fun loadContacts() {
        contactsList.clear()
        contactsList.addAll(dbHelper.getEmergencyContacts(userId))
        contactsAdapter.notifyDataSetChanged()
    }

    private fun deleteContact(contactId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to remove this emergency contact?")
            .setPositiveButton("Delete") { _, _ ->
                if (dbHelper.deleteEmergencyContact(contactId)) {
                    Toast.makeText(requireContext(), "Contact Deleted", Toast.LENGTH_SHORT).show()
                    loadContacts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // INNER RECYCLERVIEW ADAPTER CLASS
    // ==========================================
    inner class ContactsAdapter(
        private val contacts: List<Map<String, String>>,
        private val onDeleteClick: (Long) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

        inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvContactName)
            val tvRelation: TextView = view.findViewById(R.id.tvRelationship)
            val tvPhone: TextView = view.findViewById(R.id.tvPhoneNumber)
            val tvPrimary: TextView = view.findViewById(R.id.tvPrimaryBadge)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteContact)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emergency_contact, parent, false)
            return ContactViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            val contact = contacts[position]

            holder.tvName.text = contact["name"]
            holder.tvRelation.text = contact["relationship"]
            holder.tvPhone.text = contact["phone_number"]

            // Show or hide the (Primary) badge
            if (contact["is_primary"] == "1") {
                holder.tvPrimary.visibility = View.VISIBLE
            } else {
                holder.tvPrimary.visibility = View.GONE
            }

            holder.btnDelete.setOnClickListener {
                val id = contact["contact_id"]?.toLongOrNull()
                if (id != null) {
                    onDeleteClick(id)
                }
            }
        }

        override fun getItemCount(): Int = contacts.size
    }
}