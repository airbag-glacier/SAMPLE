package com.example.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BefastFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_befast, container, false)
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Top Back Button Logic
        view.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            findNavController().popBackStack()
        }

        // 2. Bottom Navigation: Camera (Global Action)
        val btnCamera = view.findViewById<FloatingActionButton>(R.id.btnCamera)
        btnCamera?.setOnClickListener {
            findNavController().navigate(R.id.action_global_scan)
        }

        // 3. Bottom Navigation: Home
        val btnHome = view.findViewById<ImageView>(R.id.btnHome)
        btnHome?.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        val webView = view.findViewById<android.webkit.WebView>(R.id.webViewWho)

        if (webView != null) {

            webView.settings.javaScriptEnabled = true


            webView.settings.domStorageEnabled = true


            webView.webViewClient = android.webkit.WebViewClient()


            webView.loadUrl("https://www.who.int/news-room/fact-sheets/detail/stroke")

            webView.setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
        }


// Bottom Navigation: Upgraded List Menu Dialog
        view.findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            val menuOptions = arrayOf("Assessment Result", "Emergency Contacts", "About / Credits", "Restart App")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("DeTechStroke Menu")
                .setItems(menuOptions) { _, which ->
                    when (which) {
                        0 -> findNavController().navigate(R.id.action_global_assessmentResult)

                        1 -> findNavController().navigate(R.id.action_global_emergencyContacts)

                        2 -> {
                            // Show the developer credits in a secondary pop-up
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("DeTechStroke")
                                .setMessage("Developers:\nGabriel Garcia\nPhoebe Andrei Quan\nNatsuki Ushijima\n\n© 2026 All Rights Reserved.")
                                .setPositiveButton("Close", null)
                                .show()
                        }

                        3 -> {
                            // Restart App Logic
                            val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                requireActivity().finish()
                            }
                        }
                    }
                }
                .setNegativeButton("Close Menu", null)
                .show()
        }
    }
}