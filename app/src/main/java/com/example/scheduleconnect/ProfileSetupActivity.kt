package com.example.scheduleconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64 // Import Base64
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
                    ivProfile.setPadding(0, 0, 0, 0) // Full bleed
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
            currentUser = sharedPref.getString("USERNAME", "") ?: "" // Changed to match other files ("USERNAME")
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
                if (currentUser.isNotEmpty()) {
                    // Disable button
                    btnSave.isEnabled = false
                    btnSave.text = "Uploading..."

                    // 1. Convert Bitmap to Base64 String
                    val base64Image = bitmapToBase64(selectedBitmap!!)

                    // 2. Call the Base64 specific method
                    dbHelper.updateProfilePictureBase64(currentUser, base64Image) { success ->
                        btnSave.isEnabled = true
                        btnSave.text = "SAVE & CONTINUE"

                        if (success) {
                            // Update SharedPreferences
                            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                            val editor = sharedPref.edit()
                            editor.putString("USERNAME", currentUser)
                            editor.apply()

                            Toast.makeText(this, "Profile Picture Saved!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        } else {
                            Toast.makeText(this, "Database Error. Try again.", Toast.LENGTH_SHORT).show()
                        }
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

    // Helper: Resize bitmap to prevent App Crash (TransactionTooLargeException)
    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val REQUIRED_SIZE = 400 // Keeping smaller for Base64 efficiency
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
            return BitmapFactory.decodeStream(inputStream, null, o2)
        } catch (e: Exception) {
            e.printStackTrace() // Log the error
            return null
        } finally {
            inputStream?.close()
        }
    }

    // Helper: Convert Bitmap to Base64 String
    private fun bitmapToBase64(bitmap: Bitmap): String {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            // CRITICAL FIX: Ensure the Bitmap is compressed safely to JPEG for efficiency
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            // Return an empty string on failure
            return ""
        }
    }
}