package com.example.scheduleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivProfile: ImageView
    private lateinit var tvUsername: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Initialize Views
        tvUsername = view.findViewById(R.id.tvSettingsUsername)
        ivProfile = view.findViewById(R.id.ivSettingsProfile)

        // 2. Navigation Buttons
        view.findViewById<Button>(R.id.btnAccount).setOnClickListener {
            replaceFragment(ViewProfileFragment())
        }

        view.findViewById<Button>(R.id.btnSecurity).setOnClickListener {
            replaceFragment(SecurityFragment())
        }

        view.findViewById<Button>(R.id.btnNotifications).setOnClickListener {
            replaceFragment(NotificationsFragment())
        }

        view.findViewById<Button>(R.id.btnLanguage).setOnClickListener {
            replaceFragment(LanguageFragment())
        }

        // 3. Logout Logic
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    // --- FIX: Refresh Data every time the Fragment appears ---
    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"

        // Set Username
        tvUsername.text = username

        // Set Profile Image
        val profileBitmap = dbHelper.getProfilePicture(username)
        if (profileBitmap != null) {
            ivProfile.setImageBitmap(profileBitmap)
            ivProfile.imageTintList = null // Remove tint if user has a custom photo
            ivProfile.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            // Reset to default if no image
            ivProfile.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}