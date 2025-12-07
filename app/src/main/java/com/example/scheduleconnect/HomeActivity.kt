package com.example.scheduleconnect

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView

class HomeActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivProfile: ShapeableImageView
    private lateinit var tvBadge: TextView // Reference to the badge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dbHelper = DatabaseHelper(this)
        ivProfile = findViewById(R.id.btnTopProfile)
        tvBadge = findViewById(R.id.tvNotificationBadge) // Init badge

        // Retrieve username and save to SharedPreferences
        val username = intent.getStringExtra("CURRENT_USER")
        if (username != null) {
            val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("username", username)
            editor.apply()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Notification Bell Click
        val btnNotif = findViewById<ImageView>(R.id.btnTopNotifications)
        btnNotif.setOnClickListener {
            loadFragment(UserNotificationsFragment())
        }

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
        updateNotificationBadge() // Check for unread notifs every time
    }

    // --- NEW: Update Badge Visibility and Count ---
    fun updateNotificationBadge() {
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPref.getString("username", "") ?: ""

        if (username.isNotEmpty()) {
            val count = dbHelper.getUnreadNotificationCount(username)
            if (count > 0) {
                tvBadge.text = if (count > 99) "99+" else count.toString()
                tvBadge.visibility = View.VISIBLE
            } else {
                tvBadge.visibility = View.GONE
            }
        }
    }

    private fun loadHeaderProfile() {
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPref.getString("username", "") ?: ""

        if (username.isNotEmpty()) {
            val bitmap = dbHelper.getProfilePicture(username)
            if (bitmap != null) {
                ivProfile.setImageBitmap(bitmap)
                ivProfile.imageTintList = null
                ivProfile.setPadding(0, 0, 0, 0)
                ivProfile.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                ivProfile.setImageResource(R.drawable.ic_person)
                val color = ContextCompat.getColor(this, R.color.app_red)
                ivProfile.imageTintList = null
                ivProfile.setColorFilter(color)
                ivProfile.setPadding(5, 5, 5, 5)
                ivProfile.scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}