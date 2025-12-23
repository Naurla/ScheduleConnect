package com.example.scheduleconnect

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var etPhone: EditText
    private lateinit var ccp: CountryCodePicker

    // Error TextViews from Layout
    private lateinit var tvFirstNameError: TextView
    private lateinit var tvLastNameError: TextView
    private lateinit var tvDobError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvPhoneError: TextView
    private lateinit var tvUsernameError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvConfirmPasswordError: TextView

    // --- EMAIL CONFIGURATION ---
    private val SENDER_EMAIL = "scheduleconnect2025@gmail.com"
    private val SENDER_PASSWORD = "zcml lkrm qeff xayy"

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
        etPhone = findViewById(R.id.etPhone)
        ccp = findViewById(R.id.ccp)
        ccp.registerCarrierNumberEditText(etPhone)

        // Initialize Error TextViews
        tvFirstNameError = findViewById(R.id.tvFirstNameError)
        tvLastNameError = findViewById(R.id.tvLastNameError)
        tvDobError = findViewById(R.id.tvDobError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPhoneError = findViewById(R.id.tvPhoneError)
        tvUsernameError = findViewById(R.id.tvUsernameError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError)

        etDob.setOnClickListener { showDatePicker() }

        rgGender.setOnCheckedChangeListener { _, checkedId ->
            etGenderOther.visibility = if (checkedId == R.id.rbOther) View.VISIBLE else View.GONE
        }

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

        tvRegister.setOnClickListener { performRegistration() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            etDob.setText(formattedDate)
            tvDobError.visibility = View.GONE // Clear error on selection
        }, year, month, day).show()
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

    private fun clearAllErrors() {
        tvFirstNameError.visibility = View.GONE
        tvLastNameError.visibility = View.GONE
        tvDobError.visibility = View.GONE
        tvEmailError.visibility = View.GONE
        tvPhoneError.visibility = View.GONE
        tvUsernameError.visibility = View.GONE
        tvPasswordError.visibility = View.GONE
        tvConfirmPasswordError.visibility = View.GONE
    }

    private fun performRegistration() {
        clearAllErrors()

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etUsername = findViewById<EditText>(R.id.etUsername)

        val fName = etFirstName.text.toString().trim()
        val lName = etLastName.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val user = etUsername.text.toString().trim()
        val pass = etPassword.text.toString().trim()
        val confirmPass = etConfirmPassword.text.toString().trim()

        // --- STEP 1 VALIDATION ---
        if (fName.isEmpty()) {
            tvFirstNameError.text = "Please enter your first name"
            tvFirstNameError.visibility = View.VISIBLE
            goToStep(0); return
        }
        if (lName.isEmpty()) {
            tvLastNameError.text = "Please enter your last name"
            tvLastNameError.visibility = View.VISIBLE
            goToStep(0); return
        }

        // --- STEP 2 VALIDATION ---
        val selectedGenderId = rgGender.checkedRadioButtonId
        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
            goToStep(1); return
        }
        if (dob.isEmpty()) {
            tvDobError.text = "Please select your date of birth"
            tvDobError.visibility = View.VISIBLE
            goToStep(1); return
        }
        if (email.isEmpty()) {
            tvEmailError.text = "An email is required for verification"
            tvEmailError.visibility = View.VISIBLE
            goToStep(1); return
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.text = "Please enter a valid email format"
            tvEmailError.visibility = View.VISIBLE
            goToStep(1); return
        }
        if (!ccp.isValidFullNumber) {
            tvPhoneError.text = "Please enter a valid phone number"
            tvPhoneError.visibility = View.VISIBLE
            goToStep(1); return
        }

        // --- STEP 3 VALIDATION ---
        if (user.isEmpty()) {
            tvUsernameError.text = "Please choose a username"
            tvUsernameError.visibility = View.VISIBLE
            goToStep(2); return
        }
        if (pass.isEmpty()) {
            tvPasswordError.text = "Please create a password"
            tvPasswordError.visibility = View.VISIBLE
            goToStep(2); return
        }
        if (pass.length < 8) {
            tvPasswordError.text = "Password must be at least 8 characters"
            tvPasswordError.visibility = View.VISIBLE
            goToStep(2); return
        }
        if (pass != confirmPass) {
            tvConfirmPasswordError.text = "The passwords you entered do not match. Please try again."
            tvConfirmPasswordError.visibility = View.VISIBLE
            goToStep(2); return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Checking availability...")
            setCancelable(false)
            show()
        }

        dbHelper.checkUsernameOrEmail(user) { exists ->
            progressDialog.dismiss()
            if (exists) {
                tvUsernameError.text = "This username is already taken"
                tvUsernameError.visibility = View.VISIBLE
                goToStep(2)
            } else {
                val phone = ccp.fullNumberWithPlus
                sendVerificationEmail(email, fName, "", lName, "", dob, phone, user, pass)
            }
        }
    }

    private fun sendVerificationEmail(email: String, fName: String, mName: String, lName: String, gender: String, dob: String, phone: String, user: String, pass: String) {
        val verificationCode = (100000..999999).random().toString()
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Sending verification code...")
            setCancelable(false)
            show()
        }

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

                // UPDATED THEME-MATCHED EMAIL TEMPLATE
                val htmlTemplate = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #F5F5F5; padding: 20px;">
                        <div style="background-color: #ffffff; padding: 40px; border-radius: 20px; text-align: center; border-top: 8px solid #8B1A1A; box-shadow: 0 4px 10px rgba(0,0,0,0.1);">
                            <h2 style="color: #8B1A1A; margin-bottom: 20px;">ScheduleConnect</h2>
                            <p style="color: #333333; font-size: 18px; margin-bottom: 20px;">Hello <b>$fName</b>!</p>
                            <p style="color: #666666; line-height: 1.6; margin-bottom: 30px;">Welcome to ScheduleConnect. To complete your registration and secure your account, please use the verification code below:</p>
                            <div style="background-color: #FDF2F2; border: 2px dashed #8B1A1A; padding: 20px; border-radius: 12px; display: inline-block; margin-bottom: 30px;">
                                <h1 style="color: #8B1A1A; font-size: 36px; font-weight: bold; letter-spacing: 8px; margin: 0;">$verificationCode</h1>
                            </div>
                            <p style="color: #999999; font-size: 12px; margin-top: 20px;">&copy; 2025 ScheduleConnect Team<br>Making scheduling simple and connected.</p>
                        </div>
                    </div>
                """.trimIndent()

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "ScheduleConnect Team"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                    subject = "Verify your Email - ScheduleConnect"
                    setContent(htmlTemplate, "text/html; charset=utf-8")
                }
                Transport.send(message)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    showVerificationDialog(verificationCode, fName, mName, lName, gender, dob, email, phone, user, pass)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@SignupActivity, "Email failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showVerificationDialog(correctCode: String, fName: String, mName: String, lName: String, gender: String, dob: String, email: String, phone: String, user: String, pass: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_verification, null)
        val alertDialog = AlertDialog.Builder(this).setView(dialogView).create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etCode = dialogView.findViewById<TextInputEditText>(R.id.etDialogCode)
        dialogView.findViewById<TextView>(R.id.btnVerifyDialog).setOnClickListener {
            if (etCode.text.toString().trim() == correctCode) {
                alertDialog.dismiss()
                saveUserToDB(fName, mName, lName, gender, dob, email, phone, user, pass)
            } else {
                etCode.error = "Incorrect code"
            }
        }
        alertDialog.show()
    }

    private fun saveUserToDB(fName: String, mName: String, lName: String, gender: String, dob: String, email: String, phone: String, user: String, pass: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Creating Account...")
            setCancelable(false)
            show()
        }
        dbHelper.addUser(fName, mName, lName, gender, dob, email, phone, user, pass) { success ->
            progressDialog.dismiss()
            if (success) {
                getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().putString("USERNAME", user).apply()
                startActivity(Intent(this, ProfileSetupActivity::class.java).apply { putExtra("CURRENT_USER", user) })
                finish()
            } else {
                Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToStep(stepIndex: Int) {
        if (viewFlipper.displayedChild != stepIndex) {
            viewFlipper.showNext() // Or logic based on stepIndex
            updateNavigationUI()
        }
    }
}