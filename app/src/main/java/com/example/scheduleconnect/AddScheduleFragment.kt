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
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AddScheduleFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etTitle: EditText
    private lateinit var tvDate: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button
    private lateinit var btnBack: ImageView

    private lateinit var ivScheduleImage: ImageView
    private lateinit var btnSelectImage: Button
    private var selectedImageBitmap: Bitmap? = null

    private val currentUser: String by lazy {
        requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
            .getString("USERNAME", "default_user") ?: "default_user"
    }

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
                    Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_schedule, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Init Views
        etTitle = view.findViewById(R.id.etSchName)
        tvDate = view.findViewById(R.id.etSchDate)
        etLocation = view.findViewById(R.id.etSchLocation)
        etDescription = view.findViewById(R.id.etSchDesc)

        btnAdd = view.findViewById(R.id.btnAddSchedule)
        btnCancel = view.findViewById(R.id.btnCancelSchedule)
        btnBack = view.findViewById(R.id.btnBackAdd)

        ivScheduleImage = view.findViewById(R.id.ivScheduleImage)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)

        // --- Logic: Back Button ---
        btnBack.setOnClickListener {
            handleBackNavigation()
        }

        // --- Logic: Cancel Button ---
        btnCancel.setOnClickListener {
            clearInputFields()
            handleBackNavigation()
        }

        btnSelectImage.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                imagePickerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Cannot open gallery", Toast.LENGTH_SHORT).show()
            }
        }

        tvDate.setOnClickListener { showDateTimeDialog() }

        btnAdd.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val dateStr = tvDate.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val desc = etDescription.text.toString().trim()

            // Validation
            if (title.isEmpty() || dateStr.isEmpty() || location.isEmpty()) {
                Toast.makeText(requireContext(), "Name, Date, and Location are required!", Toast.LENGTH_LONG).show()
                if(title.isEmpty()) etTitle.error = "Required"
                if(dateStr.isEmpty()) tvDate.error = "Required"
                if(location.isEmpty()) etLocation.error = "Required"
                return@setOnClickListener
            }

            // --- Convert Bitmap to Base64 String ---
            var base64Image = ""
            if (selectedImageBitmap != null) {
                val stream = ByteArrayOutputStream()
                selectedImageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                val byteArrays = stream.toByteArray()
                base64Image = Base64.encodeToString(byteArrays, Base64.DEFAULT)
            }

            // --- FIREBASE ASYNC SAVE ---
            btnAdd.isEnabled = false
            btnAdd.text = "Saving..."

            dbHelper.addScheduleWithBase64(currentUser, -1, title, dateStr, location, desc, "personal", base64Image) { success ->
                btnAdd.isEnabled = true
                btnAdd.text = "ADD SCHEDULE"

                if (success) {
                    // Add Notification to Firestore
                    dbHelper.addNotification(currentUser, "New Schedule Added", "You created schedule: $title", dateStr)

                    // Schedule WorkManager Alerts
                    scheduleNotification(title, dateStr)

                    Toast.makeText(requireContext(), "Personal Schedule Added!", Toast.LENGTH_SHORT).show()

                    clearInputFields()
                    handleBackNavigation()
                } else {
                    Toast.makeText(requireContext(), "Failed to add schedule. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun handleBackNavigation() {
        if (requireActivity() is HomeActivity) {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            bottomNav?.selectedItemId = R.id.nav_home
        } else {
            parentFragmentManager.popBackStack()
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
            inputStream?.close()
            return scaledBitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }

    private fun clearInputFields() {
        etTitle.text.clear()
        etLocation.text.clear()
        etDescription.text.clear()
        tvDate.text.clear()
        tvDate.hint = "Select Date and Time"
        selectedImageBitmap = null
        ivScheduleImage.setImageResource(android.R.drawable.ic_menu_gallery)
        ivScheduleImage.setPadding(20,20,20,20)
        ivScheduleImage.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#AAAAAA"))
    }

    // --- UPDATED: Schedule Notification Logic ---
    private fun scheduleNotification(title: String, dateStr: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val scheduleTime = sdf.parse(dateStr)
            val currentTime = Date()

            if (scheduleTime != null) {
                val diff = scheduleTime.time - currentTime.time

                // 1. STANDARD ALERT (At exact time of event)
                if (diff > 0) {
                    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(diff, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(
                            "title" to title,
                            "message" to "Happening Now: $title"
                        ))
                        .build()
                    WorkManager.getInstance(requireContext()).enqueue(workRequest)
                }

                // 2. ONE DAY BEFORE ALERT (Email + Notification)
                val oneDayMillis = TimeUnit.DAYS.toMillis(1)
                val diffOneDay = diff - oneDayMillis

                if (diffOneDay > 0) {
                    val reminderRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(diffOneDay, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(
                            "title" to "Upcoming: $title",
                            "message" to "This event is scheduled for tomorrow at ${dateStr.substringAfter(" ")}",
                            "FORCE_EMAIL" to true // <--- Triggers Email + Notif
                        ))
                        .build()
                    WorkManager.getInstance(requireContext()).enqueue(reminderRequest)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
