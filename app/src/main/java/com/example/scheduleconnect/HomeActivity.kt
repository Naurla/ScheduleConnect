package com.example.scheduleconnect

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
    private lateinit var tvBadge: TextView
    private lateinit var currentUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dbHelper = DatabaseHelper(this)
        ivProfile = findViewById(R.id.btnTopProfile)
        tvBadge = findViewById(R.id.tvNotificationBadge)

        // Session
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val intentUsername = intent.getStringExtra("CURRENT_USER")

        if (intentUsername != null) {
            currentUsername = intentUsername
            sharedPref.edit().putString("USERNAME", currentUsername).apply()
        } else {
            currentUsername = sharedPref.getString("USERNAME", "") ?: ""
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Notification Click
        val btnNotif = findViewById<ImageView>(R.id.btnTopNotifications)
        btnNotif.setOnClickListener {
            bottomNav.menu.setGroupCheckable(0, true, false)
            for (i in 0 until bottomNav.menu.size()) bottomNav.menu.getItem(i).isChecked = false
            bottomNav.menu.setGroupCheckable(0, true, true)
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
            if (fragment != null) loadFragment(fragment)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateNavigationHeader()
        updateNotificationBadge()
    }

    // --- UPDATED: Decode Base64 from Firestore ---
    fun updateNavigationHeader() {
        if (currentUsername.isNotEmpty()) {
            dbHelper.getUserDetails(currentUsername) { user ->
                if (user != null && user.profileImageUrl.isNotEmpty()) {
                    try {
                        // Decode Base64 String
                        val decodedByte = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)

                        ivProfile.setImageBitmap(bitmap)
                        ivProfile.setPadding(0, 0, 0, 0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        setDefaultProfileImage()
                    }
                } else {
                    setDefaultProfileImage()
                }
            }
        }
    }

    private fun setDefaultProfileImage() {
        ivProfile.setImageResource(R.drawable.ic_person)
        val color = ContextCompat.getColor(this, R.color.app_red)
        ivProfile.setColorFilter(color)
        ivProfile.setPadding(5, 5, 5, 5)
        ivProfile.scaleType = ImageView.ScaleType.FIT_CENTER
    }

     fun updateNotificationBadge() {
        if (currentUsername.isNotEmpty()) {
            dbHelper.getUnreadNotificationCount(currentUsername) { count ->
                if (count > 0) {
                    tvBadge.text = if (count > 99) "99+" else count.toString()
                    tvBadge.visibility = View.VISIBLE
                } else {
                    tvBadge.visibility = View.GONE
                }
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