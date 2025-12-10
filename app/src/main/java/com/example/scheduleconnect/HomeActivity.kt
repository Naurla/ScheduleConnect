package com.example.scheduleconnect

import android.content.Context
import android.content.Intent
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

        // Session Logic
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val intentUsername = intent.getStringExtra("CURRENT_USER")

        if (intentUsername != null) {
            currentUsername = intentUsername
            sharedPref.edit().putString("USERNAME", currentUsername).apply()
        } else {
            currentUsername = sharedPref.getString("USERNAME", "") ?: ""
        }

        if (currentUsername.isEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Handle Fragments & Navigation
        if (savedInstanceState == null) {
            val navigateTo = intent.getStringExtra("NAVIGATE_TO")
            if (navigateTo == "CHAT") {
                val groupId = intent.getIntExtra("GROUP_ID", -1)
                val groupName = intent.getStringExtra("GROUP_NAME") ?: "Group Chat"
                if (groupId != -1) {
                    val fragment = GroupChatFragment()
                    val bundle = Bundle()
                    bundle.putInt("GROUP_ID", groupId)
                    bundle.putString("GROUP_NAME", groupName)
                    fragment.arguments = bundle
                    loadFragment(fragment)
                } else {
                    loadFragment(HomeFragment())
                }
            } else {
                loadFragment(HomeFragment())
            }
        }

        // Top Bar Click Listeners
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

    fun updateNavigationHeader() {
        if (currentUsername.isNotEmpty()) {
            dbHelper.getUserDetails(currentUsername) { user ->
                if (user != null && user.profileImageUrl.isNotEmpty()) {
                    try {
                        val decodedByte = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)

                        ivProfile.post {
                            ivProfile.setImageBitmap(bitmap)
                            ivProfile.setPadding(0, 0, 0, 0)
                            ivProfile.scaleType = ImageView.ScaleType.CENTER_CROP

                            // --- FIX: CLEAR THE RED TINT ---
                            ivProfile.clearColorFilter()
                            ivProfile.imageTintList = null
                        }
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
        ivProfile.post {
            ivProfile.setImageResource(R.drawable.ic_person)
            val color = ContextCompat.getColor(this, R.color.app_red)
            ivProfile.setColorFilter(color) // This applies the Red Tint
            ivProfile.setPadding(5, 5, 5, 5)
            ivProfile.scaleType = ImageView.ScaleType.FIT_CENTER
        }
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
