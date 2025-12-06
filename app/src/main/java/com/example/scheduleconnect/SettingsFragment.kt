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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Get User Session
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"

        // 2. Setup Profile Header (Name & Image)
        val tvUsername = view.findViewById<TextView>(R.id.tvSettingsUsername)
        val ivProfile = view.findViewById<ImageView>(R.id.ivSettingsProfile)

        tvUsername.text = username

        val profileBitmap = dbHelper.getProfilePicture(username)
        if (profileBitmap != null) {
            ivProfile.setImageBitmap(profileBitmap)
        }

        // 3. Navigation Buttons
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

        // 4. Logout Logic
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
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

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}