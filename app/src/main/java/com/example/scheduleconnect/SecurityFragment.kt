package com.example.scheduleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class SecurityFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_security, container, false)

        // 1. Change Password Button
        // The ID here must match the XML: android:id="@+id/btnGoToChangePass"
        view.findViewById<Button>(R.id.btnGoToChangePass).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        // 2. Back Button
        view.findViewById<ImageView>(R.id.btnBackSecurity).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}