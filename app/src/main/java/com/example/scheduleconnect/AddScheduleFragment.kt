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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
// --- IMPORTS FOR WORK MANAGER ---
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AddScheduleFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etTitle: EditText
    private lateinit var tvDate: TextView // Assuming you use EditText ID etSchDate as TextView or EditText in layout
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnAdd: Button
    private lateinit var ivScheduleImage: ImageView
    private lateinit var btnSelectImage: Button // Ensure you have this button in XML if using this logic
    private var selectedImageBitmap: Bitmap? = null

    // Helper to get current user
    private val currentUser: String by lazy {
        requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
            .getString("username", "default_user") ?: "default_user"
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                    selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
                    ivScheduleImage.setImageBitmap(selectedImageBitmap)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_schedule, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Mapping to IDs in fragment_add_schedule.xml
        etTitle = view.findViewById(R.id.etSchName)
        tvDate = view.findViewById(R.id.etSchDate) // Using EditText as clickable field
        etLocation = view.findViewById(R.id.etSchLocation)
        etDescription = view.findViewById(R.id.etSchDesc)
        btnAdd = view.findViewById(R.id.btnAddSchedule)

        // Note: You need to add these IDs to your XML if you want the image picker
        // ivScheduleImage = view.findViewById(R.id.ivScheduleImage)
        // btnSelectImage = view.findViewById(R.id.btnSelectImage)

        tvDate.setOnClickListener { showDateTimeDialog() }

        btnAdd.setOnClickListener {
            val title = etTitle.text.toString()
            val dateStr = tvDate.text.toString()
            val location = etLocation.text.toString()
            val desc = etDescription.text.toString()

            if (title.isNotEmpty() && dateStr.isNotEmpty()) {

                var imageBytes: ByteArray? = null
                if (selectedImageBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    selectedImageBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    imageBytes = stream.toByteArray()
                }

                // Add schedule
                val success = dbHelper.addSchedule(currentUser, -1, title, dateStr, location, desc, "personal", imageBytes)

                if (success) {
                    // Notification Logic
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val scheduleTime = sdf.parse(dateStr)
                        val currentTime = Date()
                        if (scheduleTime != null) {
                            val diff = scheduleTime.time - currentTime.time
                            if (diff > 0) {
                                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                                    .setInitialDelay(diff, TimeUnit.MILLISECONDS)
                                    .setInputData(workDataOf("title" to title, "message" to "You have a schedule now!"))
                                    .build()
                                WorkManager.getInstance(requireContext()).enqueue(workRequest)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Toast.makeText(requireContext(), "Schedule Added!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to add schedule", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showDateTimeDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val formatted = String.format("%d-%02d-%02d %02d:%02d", year, month + 1, day, hour, minute)
                tvDate.setText(formatted)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
}