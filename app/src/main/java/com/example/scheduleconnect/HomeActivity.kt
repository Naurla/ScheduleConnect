package com.example.scheduleconnect

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView

class HomeActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivProfile: ShapeableImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dbHelper = DatabaseHelper(this)
        ivProfile = findViewById(R.id.btnTopProfile)

        // Retrieve username and save to SharedPreferences
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

        // --- Handle Notification Bell Click ---
        val btnNotif = findViewById<ImageView>(R.id.btnTopNotifications)
        btnNotif.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, UserNotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        // --- Handle Top Profile Icon Click ---
        ivProfile.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_settings
        }

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

    override fun onResume() {
        super.onResume()
        loadHeaderProfile()
    }

    private fun loadHeaderProfile() {
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPref.getString("username", "") ?: ""

        if (username.isNotEmpty()) {
            val bitmap = dbHelper.getProfilePicture(username)
            if (bitmap != null) {
                // User has a profile picture
                ivProfile.setImageBitmap(bitmap)
                ivProfile.imageTintList = null // Remove tint for real photo
                ivProfile.setPadding(0, 0, 0, 0)
                ivProfile.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                // No profile picture, use default icon
                ivProfile.setImageResource(R.drawable.ic_person)
                ivProfile.setColorFilter(getColor(R.color.app_red)) // Apply tint
                ivProfile.setPadding(5, 5, 5, 5) // Add padding for icon look
                ivProfile.scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}