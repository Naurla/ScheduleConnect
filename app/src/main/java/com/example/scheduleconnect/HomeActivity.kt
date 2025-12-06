package com.example.scheduleconnect

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // UPDATED: Retrieve username and save to SharedPreferences
        val username = intent.getStringExtra("CURRENT_USER")
        if (username != null) {
            val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("username", username)
            editor.apply()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Load Default Fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // --- NEW: Handle Notification Bell Click ---
        val btnNotif = findViewById<ImageView>(R.id.btnTopNotifications)
        btnNotif.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, UserNotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
        // ------------------------------------------

        bottomNav.setOnItemSelectedListener { item ->
            var fragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> fragment = HomeFragment()
                R.id.nav_add -> fragment = AddScheduleFragment()
                R.id.nav_group -> fragment = GroupFragment()
                R.id.nav_history -> fragment = HistoryFragment()
                R.id.nav_settings -> fragment = SettingsFragment()
            }
            if (fragment != null) {
                loadFragment(fragment)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}