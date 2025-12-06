package com.example.scheduleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class NotificationsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // This line "inflates" (loads) the XML design you created earlier
        return inflater.inflate(R.layout.fragment_notifications_settings, container, false)
    }
}