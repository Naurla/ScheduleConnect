package com.example.scheduleconnect

import android.app.ProgressDialog
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
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
    private lateinit var layoutEmail: LinearLayout
    private lateinit var layoutPhone: LinearLayout
    private lateinit var layoutReset: LinearLayout
    private lateinit var btnSendOTP: Button

    // Inputs
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var etNewPass: EditText
    private lateinit var etConfirmPass: EditText

    private var isEmailMethod = true
    private var targetIdentifier = ""

    // REPLACE WITH YOUR REAL EMAIL CREDENTIALS
    private val SENDER_EMAIL = "scheduleconnect2025@gmail.com"
    private val SENDER_PASSWORD = "zcml lkrm qeff xayy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        dbHelper = DatabaseHelper(this)

        // Init Views
        val rgMethod = findViewById<RadioGroup>(R.id.rgMethod)

        // These IDs must exist in the XML for the code to work
        layoutEmail = findViewById(R.id.layoutEmailInput)
        layoutPhone = findViewById(R.id.layoutPhoneInput)
        layoutReset = findViewById(R.id.layoutResetFields)

        etEmail = findViewById(R.id.etResetEmail)
        etPhone = findViewById(R.id.etResetPhone)
        ccp = findViewById(R.id.ccpReset)
        ccp.registerCarrierNumberEditText(etPhone)

        btnSendOTP = findViewById(R.id.btnSendOTP)
        etNewPass = findViewById(R.id.etNewPass)
        etConfirmPass = findViewById(R.id.etConfirmNewPass)

        findViewById<View>(R.id.btnBackForgot).setOnClickListener { finish() }

        // Toggle Logic: Show/Hide inputs based on radio selection
        rgMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbEmail) {
                isEmailMethod = true
                layoutEmail.visibility = View.VISIBLE
                layoutPhone.visibility = View.GONE
            } else {
                isEmailMethod = false
                layoutEmail.visibility = View.GONE
                layoutPhone.visibility = View.VISIBLE
            }
        }

        btnSendOTP.setOnClickListener {
            handleSendOTP()
        }

        findViewById<Button>(R.id.btnFinalizeReset).setOnClickListener {
            updatePassword()
        }
    }

    private fun handleSendOTP() {
        if (isEmailMethod) {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) { etEmail.error = "Required"; return }

            if (dbHelper.checkEmail(email)) {
                targetIdentifier = email
                sendEmailOTP(email)
            } else {
                Toast.makeText(this, "Email not found in database", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (!ccp.isValidFullNumber) { etPhone.error = "Invalid Number"; return }
            val fullPhone = ccp.fullNumberWithPlus

            if (dbHelper.checkPhone(fullPhone)) {
                targetIdentifier = fullPhone
                sendSMSOTP(fullPhone)
            } else {
                Toast.makeText(this, "Phone number not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailOTP(email: String) {
        val otp = (100000..999999).random().toString()
        val pd = ProgressDialog(this)
        pd.setMessage("Sending OTP...")
        pd.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() = PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL, "ScheduleConnect Support"))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                message.subject = "Reset Password OTP"
                message.setText("Your OTP is: $otp")

                Transport.send(message)

                withContext(Dispatchers.Main) {
                    pd.dismiss()
                    showVerificationDialog(otp)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pd.dismiss()
                    Toast.makeText(this@ForgotPasswordActivity, "Error sending Email: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendSMSOTP(phone: String) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), 1)
            return
        }

        val otp = (100000..999999).random().toString()
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phone, null, "Your OTP is: $otp", null, null)
            Toast.makeText(this, "OTP sent to $phone", Toast.LENGTH_SHORT).show()
            showVerificationDialog(otp)
        } catch (e: Exception) {
            // Fallback for demo if SMS fails (e.g. emulator)
            Toast.makeText(this, "SMS Failed (Simulated OTP: $otp)", Toast.LENGTH_LONG).show()
            showVerificationDialog(otp)
        }
    }

    private fun showVerificationDialog(correctOtp: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_verification, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etDialogCode)
        val btnVerify = dialogView.findViewById<TextView>(R.id.btnVerifyDialog)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDialog)

        dialogView.findViewById<TextView>(R.id.tvDialogSubtitle).text = "Enter the code sent to your ${if(isEmailMethod) "Email" else "Phone"}"

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnVerify.setOnClickListener {
            if (etCode.text.toString().trim() == correctOtp) {
                dialog.dismiss()
                // Show Reset Fields
                layoutReset.visibility = View.VISIBLE
                // Disable inputs
                btnSendOTP.visibility = View.GONE
                layoutEmail.visibility = View.GONE
                layoutPhone.visibility = View.GONE
                findViewById<RadioGroup>(R.id.rgMethod).visibility = View.GONE
                Toast.makeText(this, "Verified! Enter new password.", Toast.LENGTH_SHORT).show()
            } else {
                etCode.error = "Incorrect OTP"
            }
        }
        dialog.show()
    }

    private fun updatePassword() {
        val pass = etNewPass.text.toString()
        val confirm = etConfirmPass.text.toString()

        if (pass.length < 8) { etNewPass.error = "Min 8 chars"; return }
        if (pass != confirm) { etConfirmPass.error = "Passwords do not match"; return }

        val success = dbHelper.updatePassword(targetIdentifier, pass, isEmailMethod)
        if (success) {
            Toast.makeText(this, "Password Updated! Please Login.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show()
        }
    }
}