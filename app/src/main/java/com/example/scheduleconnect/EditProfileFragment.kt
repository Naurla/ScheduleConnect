package com.example.scheduleconnect

import android.app.Activity
import android.app.DatePickerDialog
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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide // Import Glide
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Calendar

class EditProfileFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etUsername: EditText
    private lateinit var etFirst: EditText
    private lateinit var etMiddle: EditText
    private lateinit var etLast: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etGender: EditText
    private lateinit var etDOB: EditText

    private lateinit var ivProfile: ImageView
    private lateinit var btnChangePhoto: TextView
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    private var selectedBitmap: Bitmap? = null
    private lateinit var currentUser: String

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    selectedBitmap = getResizedBitmap(imageUri)
                    ivProfile.setImageBitmap(selectedBitmap)
                    ivProfile.imageTintList = null
                    ivProfile.setPadding(0, 0, 0, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // Init Views
        etUsername = view.findViewById(R.id.etEditUsername)
        etFirst = view.findViewById(R.id.etEditFirstName)
        etMiddle = view.findViewById(R.id.etEditMiddleName)
        etLast = view.findViewById(R.id.etEditLastName)
        etEmail = view.findViewById(R.id.etEditEmail)
        etPhone = view.findViewById(R.id.etEditPhone)
        etGender = view.findViewById(R.id.etEditGender)
        etDOB = view.findViewById(R.id.etEditDOB)

        ivProfile = view.findViewById(R.id.ivEditProfileImage)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        btnSave = view.findViewById(R.id.btnSaveChanges)
        btnBack = view.findViewById(R.id.btnBackEditProfile)

        // Load Data
        loadCurrentData()

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        etDOB.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val date = String.format("%d-%02d-%02d", year, month + 1, day)
                etDOB.setText(date)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            saveChanges()
        }

        return view
    }

    private fun loadCurrentData() {
        // --- ASYNC FETCH ---
        dbHelper.getUserDetails(currentUser) { user ->
            if (user != null) {
                etUsername.setText(user.username)
                etFirst.setText(user.firstName)
                etMiddle.setText(user.middleName)
                etLast.setText(user.lastName)
                etEmail.setText(user.email)
                etPhone.setText(user.phone)
                etGender.setText(user.gender)
                etDOB.setText(user.dob)

                // Load Profile Picture using Glide
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivProfile)

                    ivProfile.setPadding(0, 0, 0, 0)
                    ivProfile.imageTintList = null
                }
            } else {
                Toast.makeText(context, "Failed to load user details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveChanges() {
        val fName = etFirst.text.toString().trim()
        val mName = etMiddle.text.toString().trim()
        val lName = etLast.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val gender = etGender.text.toString().trim()
        val dob = etDOB.text.toString().trim()

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty()) {
            Toast.makeText(context, "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        // 1. Update Text Info
        dbHelper.updateUserInfo(currentUser, fName, mName, lName, gender, dob, phone, email) { success ->
            if (success) {
                // 2. Check if image needs updating
                if (selectedBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    selectedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val bytes = stream.toByteArray()

                    dbHelper.updateProfilePicture(currentUser, bytes) { imgSuccess ->
                        finishUpdate(imgSuccess)
                    }
                } else {
                    finishUpdate(true)
                }
            } else {
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "SAVE CHANGES"
            }
        }
    }

    private fun finishUpdate(success: Boolean) {
        if (success) {
            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        } else {
            Toast.makeText(context, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
            btnSave.text = "SAVE CHANGES"
        }
    }

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
            val scaledBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            return scaledBitmap
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }
}