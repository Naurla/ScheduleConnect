package com.example.scheduleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Check Session (Skip login if already logged in)
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        // FIX 1: Use the consistent key "USERNAME"
        if (sharedPref.contains("USERNAME")) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        dbHelper = DatabaseHelper(this)

        // 2. Initialize Views (omitted for brevity)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)

        // 4. Login Logic
        btnLogin.setOnClickListener {
            val input = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                // UI Feedback
                btnLogin.isEnabled = false
                btnLogin.text = "Checking..."

                // Async Database Call
                dbHelper.checkUser(input, password) { success ->
                    btnLogin.isEnabled = true
                    btnLogin.text = "LOGIN"

                    if (success) {
                        // Get standardized username (handles email login)
                        dbHelper.getUsernameFromInput(input) { realUsername ->
                            val finalUser = realUsername ?: input

                            // Save Session
                            val editor = sharedPref.edit()
                            // FIX 2: Use the consistent key "USERNAME"
                            editor.putString("USERNAME", finalUser)
                            editor.apply()

                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                            // Navigate to Home
                            val intent = Intent(this, HomeActivity::class.java)
                            // Pass the username to HomeActivity for immediate use
                            intent.putExtra("CURRENT_USER", finalUser)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 5. Navigation Links (omitted for brevity)
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        tvForgot.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}