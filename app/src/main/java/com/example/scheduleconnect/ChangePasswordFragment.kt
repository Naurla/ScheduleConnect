package com.example.scheduleconnect

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ChangePasswordFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etCurrent: TextInputEditText
    private lateinit var etNew: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnUpdate: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvForgotPassword: TextView

    // Permission code for sending SMS
    private val SMS_PERMISSION_CODE = 101

    // Email Configuration
    private val SENDER_EMAIL = "scheduleconnect2025@gmail.com"
    private val SENDER_PASSWORD = "zcml lkrm qeff xayy" // App Password

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        dbHelper = DatabaseHelper(requireContext())

        etCurrent = view.findViewById(R.id.etCurrentPass)
        etNew = view.findViewById(R.id.etNewPass)
        etConfirm = view.findViewById(R.id.etConfirmPass)
        btnUpdate = view.findViewById(R.id.btnUpdatePass)
        btnBack = view.findViewById(R.id.btnBackChangePass)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Logic to open Forgot Password Activity
        tvForgotPassword.setOnClickListener {
            val intent = Intent(requireContext(), ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        btnUpdate.setOnClickListener {
            val currentPass = etCurrent.text.toString().trim()
            val newPass = etNew.text.toString().trim()
            val confirmPass = etConfirm.text.toString().trim()

            // --- 1. Basic Validation ---
            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 8) {
                Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get Current User (USING CORRECT "USERNAME" KEY)
            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val username = sharedPref.getString("USERNAME", "") ?: ""

            if (username.isNotEmpty()) {
                setLoadingState(true)

                // --- 2. Verify Old Password First ---
                dbHelper.checkUser(username, currentPass) { isValid ->
                    if (isValid) {
                        // --- 3. Get User Details (Email/Phone) for OTP ---
                        dbHelper.getUserDetails(username) { userDetails ->
                            if (userDetails != null) {
                                // Start the OTP Process
                                sendVerificationCode(userDetails, newPass)
                            } else {
                                setLoadingState(false)
                                Toast.makeText(context, "User details not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        setLoadingState(false)
                        Toast.makeText(context, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Session Error. Please relogin.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnUpdate.isEnabled = !isLoading
        btnUpdate.text = if (isLoading) "VERIFYING..." else "UPDATE PASSWORD"
    }

    // --- OTP Logic ---

    private fun sendVerificationCode(user: UserDataModel, newPass: String) {
        val otp = (100000..999999).random().toString() // Generate 6-digit OTP
        var contactMethod = ""
        var isEmail = false

        if (user.email.isNotEmpty()) {
            // Send HTML Email (Matches Theme)
            sendHtmlEmail(user.email, user.firstName, otp, newPass)
            contactMethod = user.email
            isEmail = true
        } else if (user.phone.isNotEmpty()) {
            // Send SMS
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                sendSMS(user.phone, "Your ScheduleConnect verification code is: $otp")
                contactMethod = user.phone
                isEmail = false
                showOtpDialog(otp, contactMethod, newPass, isEmail)
            } else {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
                setLoadingState(false)
                Toast.makeText(context, "SMS Permission needed to verify phone number.", Toast.LENGTH_LONG).show()
            }
        } else {
            setLoadingState(false)
            Toast.makeText(context, "No Email or Phone found to verify identity.", Toast.LENGTH_LONG).show()
        }
    }

    // --- NEW: HTML Email Function (Matches Sign Up Theme) ---
    private fun sendHtmlEmail(email: String, name: String, otp: String, newPass: String) {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Sending code to $email...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL, "ScheduleConnect Security"))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                message.subject = "Verify Password Change - ScheduleConnect"

                // BRANDED HTML TEMPLATE
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; border-radius: 10px; box-shadow: 0px 0px 10px rgba(0,0,0,0.1);">
                            <h2 style="color: #8B1A1A; text-align: center;">ScheduleConnect</h2>
                            <hr style="border: 0; border-top: 1px solid #eeeeee;">
                            <p style="font-size: 16px; color: #333333;">Hello <b>$name</b>,</p>
                            <p style="font-size: 16px; color: #555555;">
                                A request was made to change your password. Use the code below to verify this change.
                            </p>
                            <div style="background-color: #f8d7da; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0;">
                                <h1 style="color: #8B1A1A; letter-spacing: 5px; margin: 0;">$otp</h1>
                            </div>
                            <p style="font-size: 14px; color: #777777;">
                                If you did not request this change, please ignore this email. Your password will remain unchanged.
                            </p>
                            <br>
                            <p style="font-size: 14px; color: #999999; text-align: center;">
                                &copy; 2025 ScheduleConnect. All rights reserved.
                            </p>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                message.setContent(htmlContent, "text/html; charset=utf-8")
                Transport.send(message)

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    // Show the Custom Dialog after email is sent
                    showOtpDialog(otp, email, newPass, true)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    setLoadingState(false)
                    Toast.makeText(context, "Failed to send email: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "OTP sent via SMS", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            setLoadingState(false)
            Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
        }
    }

    // --- NEW: Custom Dialog Function (Matches Sign Up Theme) ---
    private fun showOtpDialog(generatedOtp: String, contactDest: String, newPass: String, isEmail: Boolean) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())

        // Inflate the SAME layout used in SignupActivity
        val dialogView = inflater.inflate(R.layout.dialog_verification, null)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        // IMPORTANT: Transparent background for rounded corners
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.setCancelable(false)

        // Bind Views from dialog_verification.xml
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)
        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etDialogCode)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDialog)
        val btnVerify = dialogView.findViewById<TextView>(R.id.btnVerifyDialog)

        // Set Dynamic Text
        tvSubtitle.text = "We sent a 6-digit code to $contactDest"

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            setLoadingState(false)
            Toast.makeText(context, "Update Cancelled", Toast.LENGTH_SHORT).show()
        }

        btnVerify.setOnClickListener {
            val enteredOtp = etCode.text.toString().trim()
            if (enteredOtp == generatedOtp) {
                alertDialog.dismiss()
                performUpdatePassword(contactDest, newPass, isEmail)
            } else {
                etCode.error = "Incorrect Code"
                etCode.requestFocus()
            }
        }

        alertDialog.show()
    }

    private fun performUpdatePassword(identifier: String, newPass: String, isEmail: Boolean) {
        btnUpdate.text = "UPDATING..."

        dbHelper.updatePassword(identifier, newPass, isEmail) { success ->
            setLoadingState(false)
            if (success) {
                Toast.makeText(context, "Password Updated Successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Failed to update password. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
