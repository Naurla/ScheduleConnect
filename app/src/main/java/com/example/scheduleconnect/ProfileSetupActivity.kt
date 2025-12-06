package com.example.scheduleconnect

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.io.ByteArrayOutputStream

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imgPreview: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var btnSave: Button
    private var selectedBitmap: Bitmap? = null
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        dbHelper = DatabaseHelper(this)
        username = intent.getStringExtra("CURRENT_USER") ?: ""

        val cardUpload = findViewById<CardView>(R.id.cardUpload)
        imgPreview = findViewById(R.id.imgProfilePreview)
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder)
        btnSave = findViewById(R.id.btnSaveProfile)
        val btnSkip = findViewById<TextView>(R.id.btnSkip)

        // 1. Pick Image Logic
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, it))
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, it)
                    }

                    // Update UI
                    imgPreview.setImageBitmap(selectedBitmap)
                    layoutPlaceholder.visibility = View.GONE
                    btnSave.visibility = View.VISIBLE
                    btnSkip.visibility = View.GONE // Hide skip if they selected a photo
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cardUpload.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 2. Save Image Logic
        btnSave.setOnClickListener {
            if (selectedBitmap != null && username.isNotEmpty()) {
                val imageBytes = bitmapToByteArray(selectedBitmap!!)
                val success = dbHelper.updateProfilePicture(username, imageBytes)

                if (success) {
                    goToHome()
                } else {
                    Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. Skip Logic
        btnSkip.setOnClickListener {
            goToHome()
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        // Resize image to keep database small (max 500x500 px)
        val resized = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.PNG, 80, stream)
        return stream.toByteArray()
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("CURRENT_USER", username)
        startActivity(intent)
        finish()
    }
}