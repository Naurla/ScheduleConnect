package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ViewProfileFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var currentUsername: String

    // --- Image Picker Launcher ---
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                saveNewProfileImage(imageUri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_view_profile, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Init Views
        tvFullName = view.findViewById(R.id.tvProfileFullName)
        tvUsername = view.findViewById(R.id.tvProfileUsername)

        val btnBack = view.findViewById<ImageView>(R.id.btnBackProfile)
        val btnChangePhoto = view.findViewById<Button>(R.id.btnChangePhoto)
        val btnChangeUsername = view.findViewById<Button>(R.id.btnChangeUsername)

        // 2. Load Data
        loadUserData()

        // 3. Actions
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnChangePhoto.setOnClickListener { showPhotoOptionsDialog() }

        btnChangeUsername.setOnClickListener { showChangeUsernameDialog() }

        return view
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUsername = sharedPref.getString("username", "") ?: ""

        if (currentUsername.isNotEmpty()) {
            // A. Load Text Info
            val user = dbHelper.getUserDetails(currentUsername)
            if (user != null) {
                tvFullName.text = "${user.firstName} ${user.lastName}".uppercase()
                tvUsername.text = user.username
            } else {
                tvFullName.text = "USER NOT FOUND"
                tvUsername.text = currentUsername
            }

            // B. Load Profile Image
            val bitmap = dbHelper.getProfilePicture(currentUsername)
            if (bitmap != null) {
                val imgView = view?.findViewById<ImageView>(R.id.ivProfileImage)
                imgView?.setImageBitmap(bitmap)
            }
        }
    }

    // --- MODAL 1: CHANGE PHOTO ---
    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update Profile Picture")
        builder.setItems(options) { dialog, which ->
            if (which == 0) {
                // Open Gallery
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                imagePickerLauncher.launch(intent)
            }
            dialog.dismiss()
        }
        builder.show()
    }

    private fun saveNewProfileImage(uri: Uri) {
        try {
            val bitmap = getResizedBitmap(uri)
            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
                val bytes = stream.toByteArray()

                val success = dbHelper.updateProfilePicture(currentUsername, bytes)
                if (success) {
                    Toast.makeText(context, "Profile Photo Updated!", Toast.LENGTH_SHORT).show()
                    loadUserData() // Refresh UI
                } else {
                    Toast.makeText(context, "Database Error", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    // --- MODAL 2: CHANGE USERNAME ---
    private fun showChangeUsernameDialog() {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_change_username, null)
        val etInput = dialogView.findViewById<EditText>(R.id.etNewUsernameInput)

        builder.setView(dialogView)
        builder.setPositiveButton("UPDATE") { dialog, _ ->
            val newName = etInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                performUsernameUpdate(newName)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("CANCEL", null)
        builder.show()
    }

    private fun performUsernameUpdate(newName: String) {
        val success = dbHelper.updateUsername(currentUsername, newName)
        if (success) {
            // 1. Update Session
            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("username", newName)
            editor.apply()

            // 2. Refresh Data
            currentUsername = newName
            Toast.makeText(context, "Username updated to $newName", Toast.LENGTH_SHORT).show()
            loadUserData()
        } else {
            Toast.makeText(context, "Username taken or invalid", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to resize image (prevent crash)
    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = requireContext().contentResolver.openInputStream(uri)
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
            inputStream = requireContext().contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream, null, o2)
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }
}