package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide // Import Glide

class SettingsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvName: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var cardProfile: CardView
    private lateinit var btnPassword: LinearLayout
    private lateinit var btnNotifications: LinearLayout
    private lateinit var btnLanguage: LinearLayout
    private lateinit var btnLogOut: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Initialize Views
        tvName = view.findViewById(R.id.tvSettingsName)
        ivProfile = view.findViewById(R.id.ivSettingsProfile)
        cardProfile = view.findViewById(R.id.cardUserProfile)

        btnPassword = view.findViewById(R.id.btnSettingPassword)
        btnNotifications = view.findViewById(R.id.btnSettingNotif)
        btnLanguage = view.findViewById(R.id.btnSettingLanguage)
        btnLogOut = view.findViewById(R.id.btnLogOut)

        // Load Data
        loadUserData()

        // Listeners
        cardProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ViewProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        btnPassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        btnNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        btnLanguage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LanguageFragment())
                .addToBackStack(null)
                .commit()
        }

        btnLogOut.setOnClickListener {
            showLogoutConfirmation()
        }

        return view
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"

        // --- ASYNC FETCH ---
        dbHelper.getUserDetails(username) { user ->
            if (user != null) {
                // Update Name
                val fullName = "${user.firstName} ${user.lastName}".trim()
                tvName.text = if (fullName.isNotEmpty()) fullName else user.username

                // Update Profile Picture using Glide
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivProfile)

                    ivProfile.imageTintList = null
                } else {
                    // Default State
                    ivProfile.setImageResource(R.drawable.ic_person)
                    ivProfile.setColorFilter(Color.parseColor("#999999"))
                }
            } else {
                tvName.text = username
            }
        }
    }

    private fun showLogoutConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)

        tvTitle.text = "LOG OUT"
        tvMessage.text = "Are you sure you want to log out?"
        btnYes.text = "YES, LOG OUT"

        btnNo.setOnClickListener { dialog.dismiss() }

        btnYes.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private fun performLogout() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()

        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}