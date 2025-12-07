package com.example.scheduleconnect

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // This forces the saved language to load before the screen is drawn
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}