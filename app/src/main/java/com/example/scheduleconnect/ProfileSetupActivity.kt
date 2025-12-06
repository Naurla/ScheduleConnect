package com.example.scheduleconnect

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import java.io.InputStream

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

        // 1. Pick Image Logic (Updated with Crash Fix)
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    // CRASH FIX: Resize image before loading into memory
                    selectedBitmap = getResizedBitmap(it)

                    if (selectedBitmap != null) {
                        // Update UI
                        imgPreview.setImageBitmap(selectedBitmap)
                        layoutPlaceholder.visibility = View.GONE
                        btnSave.visibility = View.VISIBLE
                        btnSkip.visibility = View.GONE
                    } else {
                        Toast.makeText(this, "Image format not supported", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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

    // --- CRASH FIX: HELPER TO RESIZE IMAGE ---
    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            // 1. Decode dimensions only
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // 2. Calculate scale factor (Target 500px)
            val REQUIRED_SIZE = 500
            var width_tmp = options.outWidth
            var height_tmp = options.outHeight
            var scale = 1
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) break
                width_tmp /= 2
                height_tmp /= 2
                scale *= 2
            }

            // 3. Load actual image with scaling
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = contentResolver.openInputStream(uri)
            val scaledBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            return scaledBitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        // Resize image to keep database small (max 500x500 px)
        // Note: Even though we resized for preview, we ensure it's small for DB storage here too.
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