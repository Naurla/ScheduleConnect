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
                        // --- FIX: Resolve correct username from Email or Username input ---
                        // If input is email, this returns the username. If username, it returns the username.
                        // Fallback to 'input' only if null (though checkUser passed, so it shouldn't be null).
                        val realUsername = dbHelper.getUsernameFromInput(input) ?: input

                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                        // Check profile picture using the RESOLVED username
                        val hasPic = dbHelper.hasProfilePicture(realUsername)

                        if (hasPic) {
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.putExtra("CURRENT_USER", realUsername)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, ProfileSetupActivity::class.java)
                            intent.putExtra("CURRENT_USER", realUsername)
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