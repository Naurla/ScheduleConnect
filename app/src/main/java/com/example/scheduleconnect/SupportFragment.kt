package com.example.scheduleconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SupportFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_support, container, false)

        val etSubject = view.findViewById<EditText>(R.id.etSupportSubject)
        val etMessage = view.findViewById<EditText>(R.id.etSupportMessage)
        val btnSend = view.findViewById<Button>(R.id.btnSendEmail)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackSupport)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSend.setOnClickListener {
            val subject = etSubject.text.toString().trim()
            val message = etMessage.text.toString().trim()

            if (subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            } else {
                sendEmail(subject, message)
            }
        }

        return view
    }

    private fun sendEmail(subject: String, message: String) {
        // REPLACE THIS WITH YOUR REAL EMAIL SO YOU GET THE MESSAGES
        val developerEmail = "scheduleconnect2025@example.com"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            // This ensures only email apps (Gmail, Outlook, etc.) handle the intent
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
            putExtra(Intent.EXTRA_SUBJECT, "ScheduleConnect Support: $subject")
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found on this device", Toast.LENGTH_SHORT).show()
        }
    }
}