package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
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

        // --- UPDATED HTML EMAIL TEMPLATE (MATCHING THEME) ---
        // Uses #B71C1C (App Red) for branding
        val htmlBody = """
            <div style="font-family: sans-serif; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; max-width: 600px;">
                <div style="background-color: #B71C1C; padding: 20px; text-align: center;">
                    <h2 style="color: #ffffff; margin: 0;">ScheduleConnect</h2>
                    <p style="color: #ffcdd2; margin: 5px 0 0; font-size: 14px;">Support Request</p>
                </div>
                
                <div style="padding: 20px; background-color: #ffffff;">
                    <p style="color: #333;"><strong>User:</strong> $username</p>
                    <p style="color: #333;"><strong>Subject:</strong> $subject</p>
                    
                    <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 20px 0;">
                    
                    <h3 style="color: #B71C1C; margin-top: 0;">Message</h3>
                    <div style="background-color: #f9f9f9; padding: 15px; border-left: 4px solid #B71C1C; border-radius: 4px;">
                        <p style="margin: 0; color: #555; font-size: 15px; line-height: 1.5;">$userMessage</p>
                    </div>
                    
                    <hr style="border: 0; border-top: 1px solid #eeeeee; margin: 20px 0;">
                    
                    <div style="font-size: 12px; color: #777;">
                        <p style="margin: 0; font-weight: bold;">Device Information:</p>
                        <ul style="margin-top: 5px; padding-left: 20px;">
                            <li>Device: ${Build.MANUFACTURER} ${Build.MODEL}</li>
                            <li>Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})</li>
                        </ul>
                    </div>
                </div>
                
                <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-top: 1px solid #e0e0e0;">
                    <p style="margin: 0; font-size: 11px; color: #999;">&copy; 2025 ScheduleConnect. All rights reserved.</p>
                </div>
            </div>
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html" // Ensures email apps treat this as HTML
            putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
            putExtra(Intent.EXTRA_SUBJECT, "Support: $subject")
            // Plain text fallback for older apps
            putExtra(Intent.EXTRA_TEXT, Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_LEGACY))
            // Key for modern apps (Gmail, Outlook) to render rich design
            putExtra(Intent.EXTRA_HTML_TEXT, htmlBody)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email using..."))

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
        // Reusing your generic confirmation dialog layout
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)

        tvTitle.text = "DRAFT CREATED"
        tvTitle.setTextColor(Color.parseColor("#4CAF50")) // Green for success
        tvMessage.text = "We've prepared your email.\nPlease check your email app to hit send!"

        btnYes.text = "OK"
        btnYes.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        btnNo.visibility = View.GONE

        btnYes.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
