package com.example.scheduleconnect

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker // Make sure you have this library dependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class SignupActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNext: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvRegister: TextView

    // Form Fields
    private lateinit var rgGender: RadioGroup
    private lateinit var etGenderOther: EditText
    private lateinit var etDob: EditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    // Phone & CCP
    private lateinit var etPhone: EditText
    private lateinit var ccp: CountryCodePicker

    // --- EMAIL CONFIGURATION ---
    private val SENDER_EMAIL = "scheduleconnect2025@gmail.com"
    private val SENDER_PASSWORD = "zcml lkrm qeff xayy" // App Password

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        dbHelper = DatabaseHelper(this)

        // Initialize Views
        viewFlipper = findViewById(R.id.viewFlipper)
        progressBar = findViewById(R.id.progressBar)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        tvRegister = findViewById(R.id.tvRegister)

        rgGender = findViewById(R.id.radioGroupGender)
        etGenderOther = findViewById(R.id.etGenderOther)
        etDob = findViewById(R.id.etDob)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        // Initialize Phone & CCP
        etPhone = findViewById(R.id.etPhone)
        ccp = findViewById(R.id.ccp)
        ccp.registerCarrierNumberEditText(etPhone) // Connects CCP to the EditText

        // --- FEATURE: DATE PICKER ---
        etDob.setOnClickListener {
            showDatePicker()
        }

        // LOGIC: Show/Hide "Others" Input for Gender
        rgGender.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbOther) {
                etGenderOther.visibility = View.VISIBLE
            } else {
                etGenderOther.visibility = View.GONE
            }
        }

        // Navigation Logic
        updateNavigationUI()

        btnNext.setOnClickListener {
            if (viewFlipper.displayedChild < 2) {
                viewFlipper.showNext()
                updateNavigationUI()
            }
        }

        btnBack.setOnClickListener {
            if (viewFlipper.displayedChild > 0) {
                viewFlipper.showPrevious()
                updateNavigationUI()
            } else {
                finish()
            }
        }

        tvRegister.setOnClickListener {
            performRegistration()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            etDob.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateNavigationUI() {
        val currentStep = viewFlipper.displayedChild + 1
        progressBar.progress = currentStep

        if (currentStep == 3) {
            btnNext.visibility = View.GONE
            tvRegister.visibility = View.VISIBLE
        } else {
            btnNext.visibility = View.VISIBLE
            tvRegister.visibility = View.GONE
        }
    }

    private fun performRegistration() {
        // 1. Get references
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etMiddleName = findViewById<EditText>(R.id.etMiddleName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etUsername = findViewById<EditText>(R.id.etUsername)

        // 2. Extract Strings
        val fName = etFirstName.text.toString().trim()
        val mName = etMiddleName.text.toString().trim()
        val lName = etLastName.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val user = etUsername.text.toString().trim()
        val pass = etPassword.text.toString().trim()
        val confirmPass = etConfirmPassword.text.toString().trim()

        // --- VALIDATION STEP 1: NAMES ---
        if (fName.isEmpty()) { etFirstName.error = "Required"; goToStep(0); return }
        if (lName.isEmpty()) { etLastName.error = "Required"; goToStep(0); return }

        // --- VALIDATION STEP 2: DETAILS ---
        var gender = ""
        val selectedGenderId = rgGender.checkedRadioButtonId
        if (selectedGenderId == -1) { goToStep(1); Toast.makeText(this, "Select Gender", Toast.LENGTH_SHORT).show(); return }
        else {
            val selectedRb = findViewById<RadioButton>(selectedGenderId)
            gender = selectedRb.text.toString()
            if (selectedRb.id == R.id.rbOther) {
                if (etGenderOther.text.toString().isEmpty()) { etGenderOther.error = "Specify gender"; goToStep(1); return }
                gender = etGenderOther.text.toString()
            }
        }
        if (dob.isEmpty()) { etDob.error = "Required"; goToStep(1); return }
        if (email.isEmpty()) { etEmail.error = "Required"; goToStep(1); return }

        // Phone Validation with CCP
        if (!ccp.isValidFullNumber) {
            etPhone.error = "Invalid Number";
            goToStep(1);
            return
        }
        val phone = ccp.fullNumberWithPlus // Get full international format

        // --- VALIDATION STEP 3: ACCOUNT ---
        if (user.isEmpty()) { etUsername.error = "Required"; goToStep(2); return }
        if (dbHelper.checkUsernameOrEmail(user)) { etUsername.error = "Taken"; goToStep(2); return }
        if (pass.isEmpty()) { etPassword.error = "Required"; goToStep(2); return }
        if (pass.length < 8) { etPassword.error = "Min 8 chars"; goToStep(2); return }
        if (pass != confirmPass) { etConfirmPassword.error = "No match"; goToStep(2); return }

        // --- START EMAIL VERIFICATION ---
        sendVerificationEmail(email, fName, mName, lName, gender, dob, phone, user, pass)
    }

    private fun sendVerificationEmail(
        email: String, fName: String, mName: String, lName: String,
        gender: String, dob: String, phone: String, user: String, pass: String
    ) {
        val verificationCode = (100000..999999).random().toString()

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending verification code to $email...")
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
                message.setFrom(InternetAddress(SENDER_EMAIL, "ScheduleConnect Team"))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                message.subject = "Verify your Email - ScheduleConnect"

                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; border-radius: 10px; box-shadow: 0px 0px 10px rgba(0,0,0,0.1);">
                            <h2 style="color: #8B1A1A; text-align: center;">ScheduleConnect</h2>
                            <hr style="border: 0; border-top: 1px solid #eeeeee;">
                            <p style="font-size: 16px; color: #333333;">Hello <b>$fName</b>,</p>
                            <p style="font-size: 16px; color: #555555;">
                                Thank you for registering! Please use the OTP below to complete your account verification.
                            </p>
                            <div style="background-color: #f8d7da; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0;">
                                <h1 style="color: #8B1A1A; letter-spacing: 5px; margin: 0;">$verificationCode</h1>
                            </div>
                            <p style="font-size: 14px; color: #777777;">
                                If you didn't request this code, you can safely ignore this email.
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
                    showVerificationDialog(verificationCode, fName, mName, lName, gender, dob, email, phone, user, pass)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@SignupActivity, "Failed to send email: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showVerificationDialog(
        correctCode: String, fName: String, mName: String, lName: String,
        gender: String, dob: String, email: String, phone: String, user: String, pass: String
    ) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)

        // Use custom professional layout
        val dialogView = inflater.inflate(R.layout.dialog_verification, null)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)
        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etDialogCode)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDialog)
        val btnVerify = dialogView.findViewById<TextView>(R.id.btnVerifyDialog)

        tvSubtitle.text = "We sent a 6-digit code to $email"

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnVerify.setOnClickListener {
            val enteredCode = etCode.text.toString().trim()
            if (enteredCode == correctCode) {
                alertDialog.dismiss()
                saveUserToDB(fName, mName, lName, gender, dob, email, phone, user, pass)
            } else {
                etCode.error = "Incorrect Code"
                etCode.requestFocus()
            }
        }

        alertDialog.show()
    }

    private fun saveUserToDB(
        fName: String, mName: String, lName: String,
        gender: String, dob: String, email: String, phone: String, user: String, pass: String
    ) {
        val isInserted = dbHelper.addUser(fName, mName, lName, gender, dob, email, phone, user, pass)
        if (isInserted) {
            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToStep(stepIndex: Int) {
        if (viewFlipper.displayedChild != stepIndex) {
            viewFlipper.displayedChild = stepIndex
            updateNavigationUI()
        }
    }
}