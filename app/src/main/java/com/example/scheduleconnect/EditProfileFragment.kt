package com.example.scheduleconnect

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar
import kotlin.math.max

class EditProfileFragment : Fragment() {

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnChangePhoto: TextView
    private lateinit var etUsername: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etMiddleName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etGender: EditText
    private lateinit var etDOB: EditText
    private lateinit var btnSave: Button

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences

    private var currentUsername: String = ""
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Initialize Views
        btnBack = view.findViewById(R.id.btnBackEditProfile)
        ivProfilePicture = view.findViewById(R.id.ivEditProfileImage)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)

        etUsername = view.findViewById(R.id.etEditUsername)
        etFirstName = view.findViewById(R.id.etEditFirstName)
        etMiddleName = view.findViewById(R.id.etEditMiddleName)
        etLastName = view.findViewById(R.id.etEditLastName)
        etEmail = view.findViewById(R.id.etEditEmail)
        etPhone = view.findViewById(R.id.etEditPhone)
        etGender = view.findViewById(R.id.etEditGender)
        etDOB = view.findViewById(R.id.etEditDOB)
        btnSave = view.findViewById(R.id.btnSaveChanges)

        dbHelper = DatabaseHelper(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        currentUsername = sharedPreferences.getString("USERNAME", "") ?: ""
        etUsername.isEnabled = false

        if (currentUsername.isEmpty()) {
            Toast.makeText(context, "Error: No user logged in.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        } else {
            loadUserProfile()
        }

        setupListeners()
        return view
    }

    override fun onResume() {
        super.onResume()
        // Only load from DB if we haven't picked a new image yet
        if (selectedImageUri == null) {
            loadUserProfile()
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnChangePhoto.setOnClickListener { openImagePicker() }
        btnSave.setOnClickListener { saveProfileChanges() }
        etDOB.setOnClickListener { showDatePickerDialog() }
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val date = String.format("%d-%02d-%02d", year, month + 1, day)
            etDOB.setText(date)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadUserProfile() {
        dbHelper.getUserDetails(currentUsername) { user ->
            if (user != null) {
                // Always populate text fields
                etUsername.setText(user.username)
                etFirstName.setText(user.firstName)
                etMiddleName.setText(user.middleName)
                etLastName.setText(user.lastName)
                etEmail.setText(user.email)
                etPhone.setText(user.phone)
                etGender.setText(user.gender)
                etDOB.setText(user.dob)

                // CRITICAL FIX: Only overwrite image from DB if user HAS NOT picked a new one
                if (selectedImageUri == null) {
                    if (user.profileImageUrl.isNotEmpty()) {
                        try {
                            val decodedByte = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)

                            ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
                            ivProfilePicture.setImageBitmap(bitmap)
                            ivProfilePicture.setPadding(0, 0, 0, 0)
                            ivProfilePicture.imageTintList = null
                        } catch (e: Exception) {
                            setDefaultImage()
                        }
                    } else {
                        setDefaultImage()
                    }
                }
            }
        }
    }

    private fun setDefaultImage() {
        ivProfilePicture.setImageResource(R.drawable.ic_person)
        ivProfilePicture.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private fun saveProfileChanges() {
        val fName = etFirstName.text.toString().trim()
        val mName = etMiddleName.text.toString().trim()
        val lName = etLastName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val gender = etGender.text.toString().trim()
        val dob = etDOB.text.toString().trim()

        if (fName.isEmpty() || lName.isEmpty()) {
            Toast.makeText(requireContext(), "First and Last Name are required", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        dbHelper.updateUserInfo(currentUsername, fName, mName, lName, gender, dob, phone, email) { success ->
            if (success) {
                if (selectedImageUri != null) {
                    saveImageBase64(currentUsername, selectedImageUri!!)
                } else {
                    finishUpdate()
                }
            } else {
                btnSave.isEnabled = true
                btnSave.text = "SAVE CHANGES"
                Toast.makeText(requireContext(), "Failed to update info", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageBase64(username: String, uri: Uri) {
        val base64String = encodeUriToBase64(uri)
        if (base64String != null) {
            dbHelper.updateProfilePictureBase64(username, base64String) { success ->
                if (success) {
                    finishUpdate()
                } else {
                    Toast.makeText(requireContext(), "Info saved, but image failed.", Toast.LENGTH_SHORT).show()
                    finishUpdate()
                }
            }
        } else {
            btnSave.isEnabled = true
            btnSave.text = "SAVE CHANGES"
            Toast.makeText(requireContext(), "Failed to process image.", Toast.LENGTH_LONG).show()
        }
    }

    private fun finishUpdate() {
        btnSave.isEnabled = true
        btnSave.text = "SAVE CHANGES"
        Toast.makeText(requireContext(), "Profile Updated!", Toast.LENGTH_SHORT).show()

        if (activity is HomeActivity) {
            (activity as HomeActivity).updateNavigationHeader()
        }

        parentFragmentManager.popBackStack()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // --- PREVIEW LOGIC ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data

            if (selectedImageUri != null) {
                // Load a resized bitmap for preview to avoid black screen / memory issues
                val previewBitmap = getSafeResizedBitmap(selectedImageUri!!, 500)

                if (previewBitmap != null) {
                    ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivProfilePicture.setImageBitmap(previewBitmap)
                    ivProfilePicture.setPadding(0, 0, 0, 0)
                    ivProfilePicture.imageTintList = null
                } else {
                    Toast.makeText(requireContext(), "Failed to load preview.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper: Safely load resized Bitmap
    private fun getSafeResizedBitmap(uri: Uri, targetSize: Int): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var scale = 1
            val maxDim = max(options.outWidth, options.outHeight)
            if (maxDim > targetSize) {
                scale = maxDim / targetSize
            }

            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = requireContext().contentResolver.openInputStream(uri)
            val resizedBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream?.close()

            return resizedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper: Compress for Saving
    private fun encodeUriToBase64(uri: Uri): String? {
        val bitmap = getSafeResizedBitmap(uri, 400) ?: return null
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}