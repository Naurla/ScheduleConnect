package com.example.scheduleconnect

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AddSharedScheduleFragment : Fragment() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etName: EditText
    private lateinit var etDate: EditText
    private lateinit var etLoc: EditText
    private lateinit var etDesc: EditText
    private var groupId: Int = -1

    private lateinit var ivScheduleImage: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView

    private var selectedImageBitmap: Bitmap? = null
    private var currentUser: String = ""

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

        groupId = arguments?.getInt("GROUP_ID") ?: -1

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        etName = view.findViewById(R.id.etSchName)
        etDate = view.findViewById(R.id.etSchDate)
        etLoc = view.findViewById(R.id.etSchLocation)
        etDesc = view.findViewById(R.id.etSchDesc)
        tvTitle = view.findViewById(R.id.tvAddScheduleTitle)

        val btnAdd = view.findViewById<Button>(R.id.btnAddSchedule)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelSchedule)
        btnBack = view.findViewById(R.id.btnBackAdd)

        ivScheduleImage = view.findViewById(R.id.ivScheduleImage)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)

        tvTitle.text = "ADD SHARED SCHEDULE"
        btnAdd.text = "ADD SHARED SCHEDULE"

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnCancel.setOnClickListener {
            clearInputFields()
            parentFragmentManager.popBackStack()
        }

        btnSelectImage.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                imagePickerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Cannot open gallery", Toast.LENGTH_SHORT).show()
            }
        }

        etDate.setOnClickListener { showDateTimeDialog() }

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val date = etDate.text.toString().trim()
            val loc = etLoc.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (name.isEmpty() || date.isEmpty() || loc.isEmpty()) {
                Toast.makeText(context, "Name, Date, and Location are required", Toast.LENGTH_SHORT).show()
                if(name.isEmpty()) etName.error = "Required"
                if(date.isEmpty()) etDate.error = "Required"
                if(loc.isEmpty()) etLoc.error = "Required"
                return@setOnClickListener
            }

            // --- Convert to Base64 String ---
            var base64Image = ""
            if (selectedImageBitmap != null) {
                val stream = ByteArrayOutputStream()
                selectedImageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                val byteArrays = stream.toByteArray()
                base64Image = Base64.encodeToString(byteArrays, Base64.DEFAULT)
            }

            btnAdd.isEnabled = false
            btnAdd.text = "Creating..."

            dbHelper.addScheduleWithBase64(currentUser, groupId, name, date, loc, desc, "shared", base64Image) { success ->
                btnAdd.isEnabled = true
                btnAdd.text = "ADD SHARED SCHEDULE"

                if (success) {
                    // REMOVED: notifyGroupMembers(name, date)
                    // REASON: DatabaseHelper already handles notifications for shared schedules automatically.

                    // Schedule local reminders (Exact time + 1 Day before)
                    scheduleLocalNotification(name, date, loc)

                    Toast.makeText(context, "Shared Schedule Added!", Toast.LENGTH_SHORT).show()
                    clearInputFields()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Error adding schedule", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return view
    }

    // --- UPDATED: Schedule Notification Logic ---
    private fun scheduleLocalNotification(name: String, date: String, loc: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val scheduleTime = sdf.parse(date)
            val currentTime = Date()

            if (scheduleTime != null) {
                val diff = scheduleTime.time - currentTime.time

                // 1. STANDARD ALERT
                if (diff > 0) {
                    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(diff, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(
                            "title" to "Shared Schedule: $name",
                            "message" to "Upcoming shared event at $loc"
                        ))
                        .build()
                    WorkManager.getInstance(requireContext()).enqueue(workRequest)
                }

                // 2. ONE DAY BEFORE ALERT
                val oneDayMillis = TimeUnit.DAYS.toMillis(1)
                val diffOneDay = diff - oneDayMillis

                if (diffOneDay > 0) {
                    val reminderRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                        .setInitialDelay(diffOneDay, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(
                            "title" to "Reminder: $name",
                            "message" to "Your shared event at $loc is tomorrow!",
                            "FORCE_EMAIL" to true
                        ))
                        .build()
                    WorkManager.getInstance(requireContext()).enqueue(reminderRequest)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearInputFields() {
        etName.text.clear()
        etLoc.text.clear()
        etDesc.text.clear()
        etDate.text.clear()
        etDate.hint = "Select Date and Time"
        selectedImageBitmap = null
        ivScheduleImage.setImageResource(android.R.drawable.ic_menu_gallery)
        ivScheduleImage.setPadding(20, 20, 20, 20)
        ivScheduleImage.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#AAAAAA"))
    }

    private fun showDateTimeDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val formattedDateTime = String.format("%d-%02d-%02d %02d:%02d", selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute)
                etDate.setText(formattedDateTime)
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
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }
}
