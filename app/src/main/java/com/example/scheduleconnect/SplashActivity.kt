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
            val username = sharedPref.getString("username", null)

            // 2. Decide where to go
            if (username != null && username != "default_user") {
                // User is logged in -> Go to Home
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
            } else {
                // No user found -> Go to Login
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            finish()
        }, 1000)
    }
}