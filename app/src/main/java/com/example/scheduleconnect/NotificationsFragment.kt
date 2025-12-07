package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment

class NotificationsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notifications_settings, container, false)

        // Initialize Views
        val switchDND = view.findViewById<Switch>(R.id.switchDND)
        val switchShared = view.findViewById<Switch>(R.id.switchSharedInvites)
        val switchReminders = view.findViewById<Switch>(R.id.switchScheduleReminders)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackNotif)

        // Load Saved Preferences
        val sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        switchDND.isChecked = sharedPref.getBoolean("DND_ENABLED", false)
        switchShared.isChecked = sharedPref.getBoolean("SHARED_INVITES_ENABLED", true)
        switchReminders.isChecked = sharedPref.getBoolean("SCHEDULE_REMINDERS_ENABLED", true)

        // --- Toggle Listeners ---

        // 1. Do Not Disturb
        switchDND.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("DND_ENABLED", isChecked).apply()
            val msg = if (isChecked) "DND Enabled: Notifications will be sent to Email." else "DND Disabled."
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // 2. Shared Invites (Example logic for future implementation)
        switchShared.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("SHARED_INVITES_ENABLED", isChecked).apply()
        }

        // 3. Schedule Reminders
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("SCHEDULE_REMINDERS_ENABLED", isChecked).apply()
            val msg = if (isChecked) "Reminders Enabled" else "Reminders Disabled"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}