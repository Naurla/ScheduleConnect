package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment

class NotificationsFragment : Fragment() {

    private lateinit var switchPush: Switch
    private lateinit var switchEmail: Switch
    private lateinit var switchGroup: Switch
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notifications_settings, container, false)

        // Init Views
        switchPush = view.findViewById(R.id.switchPush)
        switchEmail = view.findViewById(R.id.switchEmail)
        switchGroup = view.findViewById(R.id.switchGroup)
        btnSave = view.findViewById(R.id.btnSaveNotifSettings)
        btnBack = view.findViewById(R.id.btnBackNotifSettings)

        // 1. USE "AppSettings" TO MATCH WORKER
        val sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // 2. LOAD CORRECT KEYS
        // 'switchPush' controls whether reminders are enabled at all
        switchPush.isChecked = sharedPref.getBoolean("SCHEDULE_REMINDERS_ENABLED", true)

        // 'switchEmail' can control DND (since your worker sends emails when DND is on)
        // OR a separate email setting. For now, let's map it to a new key for clarity.
        switchEmail.isChecked = sharedPref.getBoolean("EMAIL_NOTIFICATIONS_ENABLED", false)

        // 'switchGroup' maps to group notifications
        switchGroup.isChecked = sharedPref.getBoolean("GROUP_NOTIFICATIONS_ENABLED", true)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val editor = sharedPref.edit()

            // 3. SAVE WITH CORRECT KEYS
            editor.putBoolean("SCHEDULE_REMINDERS_ENABLED", switchPush.isChecked)
            editor.putBoolean("EMAIL_NOTIFICATIONS_ENABLED", switchEmail.isChecked)
            editor.putBoolean("GROUP_NOTIFICATIONS_ENABLED", switchGroup.isChecked)

            editor.apply()

            Toast.makeText(context, "Preferences Saved", Toast.LENGTH_SHORT).show()

            // Optional: If 'switchPush' is OFF, you might want to cancel existing work
            // but usually the worker checks the flag before running, which is safer.

            parentFragmentManager.popBackStack()
        }

        return view
    }
}
