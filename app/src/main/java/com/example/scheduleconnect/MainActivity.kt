package com.example.scheduleconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Database Helper
        dbHelper = DatabaseHelper(this)

        // Initialize Views
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        // 1. Handle Sign Up Click
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // 2. Handle Login Click
        btnLogin.setOnClickListener {
            val input = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                // STEP 1: Check if the username (or email) actually exists in the DB
                // (Make sure your DatabaseHelper has this function, as we restored it in the previous step)
                val userExists = dbHelper.checkUsernameOrEmail(input)

                if (userExists) {
                    // STEP 2: The user exists, now check if the password matches
                    val checkCredentials = dbHelper.checkUser(input, password)

                    if (checkCredentials) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                        // --- UPDATED LOGIC: NAVIGATE TO DASHBOARD ---
                        val intent = Intent(this, HomeActivity::class.java)
                        // Optional: Pass the username to the dashboard if needed
                        // intent.putExtra("USERNAME", input)
                        startActivity(intent)
                        finish() // Prevents going back to login screen when pressing 'Back'
                        // --------------------------------------------

                    } else {
                        // User found, but password was wrong
                        Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // User was not found in the database at all
                    Toast.makeText(this, "Username or Email not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}