package com.example.scheduleconnect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // The ID 'bottomNav' will now be found because we created activity_home.xml
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Load Default Fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}