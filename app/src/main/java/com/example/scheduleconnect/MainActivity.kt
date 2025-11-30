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
                // Check DB for Username OR Email match
                val checkUser = dbHelper.checkUser(input, password)
                if (checkUser) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                    // Navigate to your Home Screen / Dashboard here
                    // val intent = Intent(this, HomeActivity::class.java)
                    // startActivity(intent)
                    // finish()
                } else {
                    Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}