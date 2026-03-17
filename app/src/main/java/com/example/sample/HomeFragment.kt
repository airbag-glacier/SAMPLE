package com.example.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Camera Button
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }

        // 2. Setup Checkup Card
        val cardCheckup = view.findViewById<View>(R.id.cardCheckup)
        cardCheckup?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_checkupFragment)
        }

        // 3. Setup Vitals Card
        val cardVitals = view.findViewById<View>(R.id.cardVitals)
        cardVitals?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_vitals)
        }

        // 4. Setup BE-FAST Card
        val cardBefast = view.findViewById<View>(R.id.cardBefast)
        cardBefast?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_befast)
        }

        // 5. Setup Blood Chem Card
        val cardBloodChem = view.findViewById<View>(R.id.cardBloodChem)
        cardBloodChem?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_bloodChem)
        }

        // 6. Setup Risk Factors Card
        val cardRiskFactors = view.findViewById<View>(R.id.cardRiskFactors)
        cardRiskFactors?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_riskFactors)
        }

        // --- DYNAMICALLY LOAD USER PROFILE ---
        val dbHelper = DatabaseHelper(requireContext())
        val userEmail = requireActivity().intent.getStringExtra("USER_EMAIL")

        if (userEmail != null) {
            val userData = dbHelper.getUserData(userEmail)
            if (userData != null) {
                // Set Name
                view.findViewById<TextView>(R.id.tvName).text = userData["name"]

                // Set Profile Image
                val imageUriString = userData["image_uri"]
                if (!imageUriString.isNullOrEmpty()) {
                    val uri = imageUriString.toUri()
                    view.findViewById<ImageView>(R.id.imgProfile).setImageURI(uri)
                }
            } // ... (Keep your existing profile image code above this) ...

            // --- DYNAMICALLY LOAD VITALS & RISK FACTORS ---
            val healthSummary = dbHelper.getUserHealthSummary()
            view.findViewById<TextView>(R.id.tvDetails).text = healthSummary

            // --- MAKE "SEE DETAILS >" CLICKABLE ---
            view.findViewById<TextView>(R.id.tvSeeDetails).setOnClickListener {
                findNavController().navigate(R.id.action_home_to_vitals)
            }
        }
    }
}