package com.example.scheduleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Check for saved session
            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            // FIX 1: Use the consistent key "USERNAME"
            val username = sharedPref.getString("USERNAME", null)

            // 2. Decide where to go
            // FIX 2: Check if username is not null AND not empty
            if (username != null && username.isNotEmpty()) {
                // User is logged in -> Go to Home
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("CURRENT_USER", username) // Pass user for session consistency
                startActivity(intent)
            } else {
                // No user found -> Go to Login
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            finish()
        }, 3000)
    }
}