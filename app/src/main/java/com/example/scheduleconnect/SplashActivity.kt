package com.example.scheduleconnect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This links to your Splash XML layout
        setContentView(R.layout.activity_splash)

        // 5 second timer
        Handler(Looper.getMainLooper()).postDelayed({
            // Now "MainActivity" exists, so this line will work!
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }
}