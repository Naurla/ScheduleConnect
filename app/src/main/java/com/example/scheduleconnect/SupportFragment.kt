package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html // Import HTML
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SupportFragment : Fragment() {

    private lateinit var etSubject: EditText
    private lateinit var etMessage: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_support, container, false)

        etSubject = view.findViewById(R.id.etSupportSubject)
        etMessage = view.findViewById(R.id.etSupportMessage)
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
                sendHtmlEmail(subject, message)
            }
        }

        return view
    }

    private fun sendHtmlEmail(subject: String, userMessage: String) {
        val developerEmail = "scheduleconnect2025@gmail.com"

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("USERNAME", "Unknown User") ?: "Unknown User"

        // --- HTML EMAIL TEMPLATE ---
        // This creates a professional look similar to the OTP email
        val htmlBody = """
            <h2 style="color:#B71C1C;">ScheduleConnect Support</h2>
            <p><b>From:</b> $username</p>
            <hr>
            <h3>Message:</h3>
            <p style="font-size:14px;">$userMessage</p>
            <br>
            <hr>
            <p style="color:gray; font-size:12px;">
                <b>Device Info:</b> ${Build.MANUFACTURER} ${Build.MODEL}<br>
                <b>Android OS:</b> ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            </p>
        """.trimIndent()

        // Use ACTION_SEND to support HTML content better than ACTION_SENDTO
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html" // Important: Tells the app this is HTML
            putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
            putExtra(Intent.EXTRA_SUBJECT, "Support: $subject")
            // Fallback for older email apps that don't support HTML
            putExtra(Intent.EXTRA_TEXT, Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_LEGACY))
            // This is the key for modern email apps to render the design
            putExtra(Intent.EXTRA_HTML_TEXT, htmlBody)
        }

        try {
            // Force user to pick an email app
            startActivity(Intent.createChooser(intent, "Send Email..."))

            // Show success overlay
            showSuccessDialog()

            // Clear inputs
            etSubject.text.clear()
            etMessage.text.clear()

        } catch (e: Exception) {
            Toast.makeText(context, "No email app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)

        tvTitle.text = "SUCCESS"
        tvTitle.setTextColor(Color.parseColor("#4CAF50"))
        tvMessage.text = "Your email draft has been created. Please press send in your email app."

        btnYes.text = "OK, GOT IT"
        btnYes.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        btnNo.visibility = View.GONE

        btnYes.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}