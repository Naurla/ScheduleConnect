package com.example.scheduleconnect

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ChangePasswordFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)

        // FIX: Updated ID from 'tvForgotPassword' to 'tvForgotPasswordLink' to match the new XML
        view.findViewById<TextView>(R.id.tvForgotPasswordLink).setOnClickListener {
            val intent = Intent(activity, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.btnSavePassword).setOnClickListener {
            // Add your password update logic here
            Toast.makeText(context, "Password Changed Successfully", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        // Back Button Logic
        view.findViewById<ImageView>(R.id.btnBackChangePass).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}