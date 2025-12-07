package com.example.scheduleconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivProfile: ImageView
    private lateinit var btnUpload: Button
    private lateinit var btnSave: Button
    private lateinit var tvSkip: TextView

    private var selectedBitmap: Bitmap? = null
    private var currentUser: String = ""

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    selectedBitmap = getResizedBitmap(imageUri)
                    ivProfile.setImageBitmap(selectedBitmap)
                    ivProfile.imageTintList = null // Remove tint
                    ivProfile.setPadding(0,0,0,0) // Full bleed
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        dbHelper = DatabaseHelper(this)

        // Retrieve username passed from MainActivity or Login
        currentUser = intent.getStringExtra("CURRENT_USER") ?: ""

        // Fallback: Try fetching from SharedPreferences if Intent is empty
        if (currentUser.isEmpty()) {
            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            currentUser = sharedPref.getString("username", "") ?: ""
        }

        ivProfile = findViewById(R.id.ivSetupProfileImage)
        btnUpload = findViewById(R.id.btnSetupUpload)
        btnSave = findViewById(R.id.btnSetupSave)
        tvSkip = findViewById(R.id.tvSetupSkip)

        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        btnSave.setOnClickListener {
            if (selectedBitmap != null) {
                val stream = ByteArrayOutputStream()
                selectedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val imageBytes = stream.toByteArray()

                if (currentUser.isNotEmpty()) {
                    val success = dbHelper.updateProfilePicture(currentUser, imageBytes)
                    if (success) {
                        // --- FIX: Update SharedPreferences IMMEDIATELY here ---
                        // This guarantees HomeActivity knows the user is logged in
                        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("username", currentUser)
                        editor.apply()
                        // ----------------------------------------------------

                        Toast.makeText(this, "Profile Picture Saved!", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    } else {
                        Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        tvSkip.setOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("CURRENT_USER", currentUser)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

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

            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = contentResolver.openInputStream(uri)
            val scaledBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            return scaledBitmap
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }
}