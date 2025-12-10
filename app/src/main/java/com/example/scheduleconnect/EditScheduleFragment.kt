package com.example.scheduleconnect

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide // Ensure Glide is imported
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditScheduleFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etTitle: EditText
    private lateinit var etDate: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnBack: ImageView
    private lateinit var ivScheduleImage: ImageView
    private lateinit var btnSelectImage: Button
    private var selectedImageBitmap: Bitmap? = null

    private var schId: Int = -1
    private var currentSchedule: Schedule? = null // Store the loaded schedule

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    selectedImageBitmap = getResizedBitmap(imageUri)
                    ivScheduleImage.setImageBitmap(selectedImageBitmap)
                    ivScheduleImage.setPadding(0, 0, 0, 0)
                    ivScheduleImage.imageTintList = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_edit_schedule, container, false)
        dbHelper = DatabaseHelper(requireContext())

        schId = arguments?.getInt("SCH_ID") ?: -1

        // Init Views
        etTitle = view.findViewById(R.id.etEditSchName)
        etDate = view.findViewById(R.id.etEditSchDate)
        etLocation = view.findViewById(R.id.etEditSchLocation)
        etDescription = view.findViewById(R.id.etEditSchDesc)
        btnUpdate = view.findViewById(R.id.btnUpdateSchedule)
        btnCancel = view.findViewById(R.id.btnCancelEdit)
        btnBack = view.findViewById(R.id.btnBackEdit)
        ivScheduleImage = view.findViewById(R.id.ivEditScheduleImage)
        btnSelectImage = view.findViewById(R.id.btnEditSelectImage)

        // --- ASYNC LOAD DATA ---
        // Instead of immediate return, we wait for the callback
        dbHelper.getSchedule(schId) { schedule ->
            if (schedule != null) {
                currentSchedule = schedule // Save for later use (notifications)

                etTitle.setText(schedule.title)
                etDate.setText(schedule.date)
                etLocation.setText(schedule.location)
                etDescription.setText(schedule.description)

                // Load image using Glide if URL exists
                if (schedule.imageUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(schedule.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(ivScheduleImage)

                    ivScheduleImage.setPadding(0, 0, 0, 0)
                    ivScheduleImage.imageTintList = null
                }
            } else {
                Toast.makeText(context, "Error loading schedule", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        // --- Listeners ---
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        etDate.setOnClickListener { showDateTimeDialog() }

        btnUpdate.setOnClickListener {
            val newTitle = etTitle.text.toString().trim()
            val newDate = etDate.text.toString().trim()
            val newLoc = etLocation.text.toString().trim()
            val newDesc = etDescription.text.toString().trim()

            if (newTitle.isNotEmpty() && newDate.isNotEmpty()) {
                // Disable button to prevent double clicks
                btnUpdate.isEnabled = false
                btnUpdate.text = "Updating..."

                var imageBytes: ByteArray? = null
                if (selectedImageBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    selectedImageBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    imageBytes = stream.toByteArray()
                }

                // --- ASYNC UPDATE ---
                dbHelper.updateScheduleDetails(schId, newTitle, newDate, newLoc, newDesc, imageBytes) { success ->
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "UPDATE SCHEDULE"

                    if (success) {
                        // Notify Attendees if Shared
                        if (currentSchedule != null && currentSchedule!!.type == "shared") {
                            notifyAttendeesOfUpdate(currentSchedule!!.groupId, currentSchedule!!.creator, newTitle, newDate, newLoc)
                        }

                        Toast.makeText(requireContext(), "Schedule Updated!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Name and Date required", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun notifyAttendeesOfUpdate(groupId: Int, creator: String, title: String, date: String, loc: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val notifTitle = "Schedule Updated: $title"
        val notifMessage = "Details have changed. New Date: $date, Loc: $loc"

        // 1. Get members asynchronously
        dbHelper.getGroupMemberUsernames(groupId, creator) { memberUsernames ->
            // 2. Loop and notify via App
            for (user in memberUsernames) {
                dbHelper.addNotification(user, notifTitle, notifMessage, currentDate)
            }

            // 3. (Optional) Email notification logic would go here if enabled
        }
    }

    private fun showDateTimeDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val formatted = String.format("%d-%02d-%02d %02d:%02d", year, month + 1, day, hour, minute)
                etDate.setText(formatted)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
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