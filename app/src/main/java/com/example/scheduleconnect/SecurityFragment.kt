package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class SecurityFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvEmail: TextView
    private lateinit var btnToggle: ImageView

    private var fullEmail: String = ""
    private var isEmailVisible: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_security, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Initialize Views
        tvEmail = view.findViewById(R.id.tvSecurityEmail)
        btnToggle = view.findViewById(R.id.btnToggleEmailVisibility)

        // 1. Load User Data
        loadUserData()

        // 2. Toggle Visibility Click Listener
        btnToggle.setOnClickListener {
            isEmailVisible = !isEmailVisible
            updateEmailDisplay()
        }

        // 3. Navigation Buttons
        view.findViewById<Button>(R.id.btnGoToChangePass).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<ImageView>(R.id.btnBackSecurity).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "") ?: ""

        if (username.isNotEmpty()) {
            // --- ASYNC FETCH ---
            dbHelper.getUserDetails(username) { user ->
                if (user != null) {
                    fullEmail = user.email
                    // Start with hidden state
                    isEmailVisible = false
                    updateEmailDisplay()
                } else {
                    tvEmail.text = "Email not found"
                }
            }
        }
    }

    private fun updateEmailDisplay() {
        if (isEmailVisible) {
            tvEmail.text = fullEmail
            btnToggle.alpha = 1.0f // Fully opaque when visible
//            btnToggle.setImageResource(R.drawable.ic_eye_off) // Optional: Change icon if you have ic_eye_off
        } else {
            tvEmail.text = maskEmail(fullEmail)
            btnToggle.alpha = 0.5f // Semi-transparent when hidden
//            btnToggle.setImageResource(R.drawable.ic_eye) // Optional: Change icon if you have ic_eye
        }
    }

    private fun maskEmail(email: String): String {
        if (email.isEmpty()) return ""
        val atIndex = email.indexOf("@")
        if (atIndex <= 1) return email // Return as is if too short to mask

        // e.g., j*******@gmail.com
        val domain = email.substring(atIndex)
        val namePart = email.substring(0, atIndex)
        val firstChar = namePart.first()

        return "$firstChar**********$domain"
    }
}