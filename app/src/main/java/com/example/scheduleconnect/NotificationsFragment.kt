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

        // Load Saved Preferences
        val sharedPref = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        switchPush.isChecked = sharedPref.getBoolean("notif_push", true)
        switchEmail.isChecked = sharedPref.getBoolean("notif_email", false)
        switchGroup.isChecked = sharedPref.getBoolean("notif_group", true)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putBoolean("notif_push", switchPush.isChecked)
            editor.putBoolean("notif_email", switchEmail.isChecked)
            editor.putBoolean("notif_group", switchGroup.isChecked)
            editor.apply()

            Toast.makeText(context, "Preferences Saved", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        return view
    }
}