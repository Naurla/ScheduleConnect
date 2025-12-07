package com.example.scheduleconnect

import android.content.Context
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

        dbHelper = DatabaseHelper(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        tvForgot.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val input = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                val userExists = dbHelper.checkUsernameOrEmail(input)

                if (userExists) {
                    val checkCredentials = dbHelper.checkUser(input, password)

                    if (checkCredentials) {
                        val realUsername = dbHelper.getUsernameFromInput(input) ?: input

                        // Save Session
                        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("username", realUsername)
                        editor.apply()

                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                        val hasPic = dbHelper.hasProfilePicture(realUsername)

                        if (hasPic) {
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, ProfileSetupActivity::class.java)
                            startActivity(intent)
                        }
                        finish()

                    } else {
                        Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Username or Email not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}