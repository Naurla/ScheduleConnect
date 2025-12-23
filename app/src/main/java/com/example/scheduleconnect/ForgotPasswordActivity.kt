package com.example.scheduleconnect

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etEmail: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var btnSendOTP: Button
    private lateinit var layoutReset: LinearLayout
    private lateinit var layoutEmailInput: LinearLayout

    private val SENDER_EMAIL = "scheduleconnect2025@gmail.com"
    private val SENDER_PASSWORD = "zcml lkrm qeff xayy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        dbHelper = DatabaseHelper(this)

        etEmail = findViewById(R.id.etResetEmail)
        tvEmailError = findViewById(R.id.tvEmailError)
        btnSendOTP = findViewById(R.id.btnSendOTP)
        layoutReset = findViewById(R.id.layoutResetFields)
        layoutEmailInput = findViewById(R.id.layoutEmailInput)

        findViewById<View>(R.id.btnBackForgot).setOnClickListener { finish() }

        btnSendOTP.setOnClickListener { handleSendOTP() }

        findViewById<Button>(R.id.btnFinalizeReset).setOnClickListener { updatePassword() }
    }

    private fun handleSendOTP() {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.text = "Please enter a valid email address"
            tvEmailError.visibility = View.VISIBLE
            return
        }
        tvEmailError.visibility = View.GONE

        val pd = ProgressDialog(this).apply { setMessage("Verifying account..."); show() }

        dbHelper.checkEmail(email) { exists ->
            pd.dismiss()
            if (exists) {
                sendEmailOTP(email)
            } else {
                tvEmailError.text = "This email is not registered"
                tvEmailError.visibility = View.VISIBLE
            }
        }
    }

    private fun sendEmailOTP(email: String) {
        val otp = (100000..999999).random().toString()
        val pd = ProgressDialog(this).apply { setMessage("Sending reset code..."); show() }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() = PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                })

                val htmlTemplate = """
                    <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; background-color: #F5F5F5; padding: 20px;">
                        <div style="background-color: #ffffff; padding: 40px; border-radius: 20px; text-align: center; border-top: 8px solid #8B1A1A;">
                            <h2 style="color: #8B1A1A;">Password Reset Request</h2>
                            <p>You requested to reset your ScheduleConnect password. Use the code below to proceed:</p>
                            <div style="background-color: #FDF2F2; border: 2px dashed #8B1A1A; padding: 20px; border-radius: 12px; display: inline-block; margin: 20px 0;">
                                <h1 style="color: #8B1A1A; font-size: 36px; letter-spacing: 8px; margin: 0;">$otp</h1>
                            </div>
                            <p style="color: #999999; font-size: 12px;">If you didn't request this, you can safely ignore this email.</p>
                        </div>
                    </div>
                """.trimIndent()

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "ScheduleConnect Support"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                    subject = "Your Password Reset Code"
                    setContent(htmlTemplate, "text/html; charset=utf-8")
                }
                Transport.send(message)

                withContext(Dispatchers.Main) {
                    pd.dismiss()
                    showVerificationDialog(otp, email)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pd.dismiss()
                    Toast.makeText(this@ForgotPasswordActivity, "Email failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showVerificationDialog(correctOtp: String, email: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_verification, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etCode = view.findViewById<TextInputEditText>(R.id.etDialogCode)
        view.findViewById<TextView>(R.id.tvDialogSubtitle).text = "Enter the code sent to $email"

        view.findViewById<TextView>(R.id.btnVerifyDialog).setOnClickListener {
            if (etCode.text.toString().trim() == correctOtp) {
                dialog.dismiss()
                layoutReset.visibility = View.VISIBLE
                btnSendOTP.visibility = View.GONE
                layoutEmailInput.visibility = View.GONE
            } else {
                etCode.error = "Incorrect code"
            }
        }
        dialog.show()
    }

    private fun updatePassword() {
        val pass = findViewById<EditText>(R.id.etNewPass).text.toString()
        val confirm = findViewById<EditText>(R.id.etConfirmNewPass).text.toString()
        val email = etEmail.text.toString().trim()

        if (pass.length < 8) { Toast.makeText(this, "Minimum 8 characters", Toast.LENGTH_SHORT).show(); return }
        if (pass != confirm) { Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show(); return }

        val pd = ProgressDialog(this).apply { setMessage("Updating password..."); show() }
        dbHelper.updatePassword(email, pass, true) { success ->
            pd.dismiss()
            if (success) {
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}