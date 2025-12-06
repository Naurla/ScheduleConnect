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

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val input = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                val userExists = dbHelper.checkUsernameOrEmail(input)

                if (userExists) {
                    val checkCredentials = dbHelper.checkUser(input, password)

                    if (checkCredentials) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                        // --- UPDATED LOGIC START ---
                        val hasPic = dbHelper.hasProfilePicture(input)

                        if (hasPic) {
                            // Proceed to Home directly
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.putExtra("CURRENT_USER", input)
                            startActivity(intent)
                        } else {
                            // Redirect to Profile Setup
                            val intent = Intent(this, ProfileSetupActivity::class.java)
                            intent.putExtra("CURRENT_USER", input)
                            startActivity(intent)
                        }
                        finish()
                        // --- UPDATED LOGIC END ---

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