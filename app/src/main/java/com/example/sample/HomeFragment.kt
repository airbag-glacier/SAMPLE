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


        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scan)
        }


        val cardCheckup = view.findViewById<View>(R.id.cardCheckup)
        cardCheckup?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_checkupFragment)
        }


        val cardVitals = view.findViewById<View>(R.id.cardVitals)
        cardVitals?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_vitals)
        }


        val cardBefast = view.findViewById<View>(R.id.cardBefast)
        cardBefast?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_befast)
        }


        val cardBloodChem = view.findViewById<View>(R.id.cardBloodChem)
        cardBloodChem?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_bloodChem)
        }


        val cardRiskFactors = view.findViewById<View>(R.id.cardRiskFactors)
        cardRiskFactors?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_riskFactors)
        }


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
            }


            val healthSummary = dbHelper.getUserHealthSummary()
            view.findViewById<TextView>(R.id.tvDetails).text = healthSummary

        }

        view.findViewById<TextView>(R.id.tvSeeDetails).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profileDetails)
        }
    }


}
