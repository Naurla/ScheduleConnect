package com.example.scheduleconnect

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNext: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvRegister: TextView

    // Step 2 Fields (needed for logic)
    private lateinit var rgGender: RadioGroup
    private lateinit var etGenderOther: EditText

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

        // LOGIC: Show/Hide "Others" Input
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
                finish() // Close activity if back is pressed on first page
            }
        }

        tvRegister.setOnClickListener {
            performRegistration()
        }
    }

    private fun updateNavigationUI() {
        val currentStep = viewFlipper.displayedChild + 1
        progressBar.progress = currentStep

        // Show "Register" text instead of Arrow on last step (Step 3)
        if (currentStep == 3) {
            btnNext.visibility = View.GONE
            tvRegister.visibility = View.VISIBLE
        } else {
            btnNext.visibility = View.VISIBLE
            tvRegister.visibility = View.GONE
        }
    }

    private fun performRegistration() {
        // 1. Gather Data
        val fName = findViewById<EditText>(R.id.etFirstName).text.toString()
        val mName = findViewById<EditText>(R.id.etMiddleName).text.toString()
        val lName = findViewById<EditText>(R.id.etLastName).text.toString()
        val dob = findViewById<EditText>(R.id.etDob).text.toString()
        val email = findViewById<EditText>(R.id.etEmail).text.toString()
        val phone = findViewById<EditText>(R.id.etPhone).text.toString()
        val user = findViewById<EditText>(R.id.etUsername).text.toString()
        val pass = findViewById<EditText>(R.id.etPassword).text.toString()
        val confirmPass = findViewById<EditText>(R.id.etConfirmPassword).text.toString()

        // Gender Logic
        var gender = ""
        val selectedGenderId = rgGender.checkedRadioButtonId
        if (selectedGenderId != -1) {
            val selectedRb = findViewById<RadioButton>(selectedGenderId)
            gender = selectedRb.text.toString()
            if (selectedRb.id == R.id.rbOther) {
                gender = etGenderOther.text.toString() // Use the typed input
            }
        }

        // 2. Validation (Basic)
        if (fName.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Save to SQLite
        val isInserted = dbHelper.addUser(fName, mName, lName, gender, dob, email, phone, user, pass)
        if (isInserted) {
            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
            finish() // Return to Login
        } else {
            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
        }
    }
}