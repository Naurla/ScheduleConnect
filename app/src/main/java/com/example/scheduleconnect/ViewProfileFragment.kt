package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.imageview.ShapeableImageView
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ViewProfileFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var currentUsername: String

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

        tvFullName = view.findViewById(R.id.tvProfileFullName)
        tvUsername = view.findViewById(R.id.tvProfileUsername)
        ivProfileImage = view.findViewById(R.id.ivProfileImage)

        val btnBack = view.findViewById<ImageView>(R.id.btnBackProfile)
        val btnChangePhoto = view.findViewById<Button>(R.id.btnChangePhoto)
        val btnChangeUsername = view.findViewById<Button>(R.id.btnChangeUsername)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnChangePhoto.setOnClickListener { showPhotoOptionsDialog() }
        btnChangeUsername.setOnClickListener { showChangeUsernameDialog() }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        if (activity == null) return
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUsername = sharedPref.getString("username", "") ?: ""

        if (currentUsername.isNotEmpty()) {
            val user = dbHelper.getUserDetails(currentUsername)
            if (user != null) {
                tvFullName.text = "${user.firstName} ${user.lastName}".uppercase()
                tvUsername.text = "@${user.username}"
            }
            val bitmap = dbHelper.getProfilePicture(currentUsername)
            if (bitmap != null) {
                ivProfileImage.setImageBitmap(bitmap)
            } else {
                ivProfileImage.setImageResource(android.R.color.darker_gray)
            }
        }
    }

    // --- HELPER: SHOW CUSTOM MODAL ---
    private fun showCustomModal(title: String, message: String, onOkClick: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_custom_modal, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnOk = view.findViewById<Button>(R.id.btnDialogOk)

        tvTitle.text = title
        tvMessage.text = message

        btnOk.setOnClickListener {
            dialog.dismiss()
            onOkClick?.invoke()
        }
        dialog.show()
    }

    // --- MODAL 1: CHANGE PHOTO ---
    private fun showPhotoOptionsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_photo_options, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnGallery = dialogView.findViewById<Button>(R.id.btnDialogGallery)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnDialogCancel)

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
            dialog.dismiss()
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun saveNewProfileImage(uri: Uri) {
        try {
            val bitmap = getResizedBitmap(uri)
            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                val bytes = stream.toByteArray()

                val success = dbHelper.updateProfilePicture(currentUsername, bytes)
                if (success) {
                    // UPDATED: Show Custom Modal instead of Toast
                    showCustomModal("Success!", "Profile picture has been updated.")
                    ivProfileImage.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(context, "Database Error", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            sharedPref.edit().putString("username", newName).apply()
            currentUsername = newName

            // UPDATED: Show Custom Modal instead of Toast
            showCustomModal("Success!", "Username updated to $newName") {
                loadUserData()
            }
        } else {
            // Error Modal
            showCustomModal("Error", "Username is already taken or invalid.")
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

            val REQUIRED_SIZE = 400
            var scale = 1
            while (options.outWidth / scale / 2 >= REQUIRED_SIZE && options.outHeight / scale / 2 >= REQUIRED_SIZE) {
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