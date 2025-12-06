package com.example.scheduleconnect

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.Calendar
// --- IMPORTS FOR WORK MANAGER ---
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddSharedScheduleFragment : Fragment() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etDate: EditText
    private var groupId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_schedule, container, false)
        dbHelper = DatabaseHelper(requireContext())
        groupId = arguments?.getInt("GROUP_ID") ?: -1

        val etName = view.findViewById<EditText>(R.id.etSchName)
        etDate = view.findViewById<EditText>(R.id.etSchDate)
        val etLoc = view.findViewById<EditText>(R.id.etSchLocation)
        val etDesc = view.findViewById<EditText>(R.id.etSchDesc)
        val btnAdd = view.findViewById<Button>(R.id.btnAddSchedule)

        // Change text to match design
        btnAdd.text = "ADD SHARED SCHEDULE"

        etDate.setOnClickListener { showDateTimeDialog() }

        btnAdd.setOnClickListener {
            val name = etName.text.toString()
            val date = etDate.text.toString()
            val loc = etLoc.text.toString()
            val desc = etDesc.text.toString()

            if (name.isNotEmpty() && date.isNotEmpty()) {
                val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

                // Assuming optional image is null for shared schedules for now, or you can add image picker logic here too
                val success = dbHelper.addSchedule(currentUser, groupId, name, date, loc, desc, "shared")

                if (success) {
                    // --- NOTIFICATION LOGIC ---
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val scheduleTime = sdf.parse(date)
                        val currentTime = Date()

                        if (scheduleTime != null) {
                            val diff = scheduleTime.time - currentTime.time
                            if (diff > 0) {
                                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                                    .setInitialDelay(diff, TimeUnit.MILLISECONDS)
                                    .setInputData(workDataOf("title" to "Shared Schedule: $name", "message" to "Upcoming shared event at $loc"))
                                    .build()
                                WorkManager.getInstance(requireContext()).enqueue(workRequest)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // --------------------------

                    Toast.makeText(context, "Shared Schedule Added!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Error adding schedule", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Name and Date required", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun showDateTimeDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                // Ensure this format matches SimpleDateFormat above
                val formattedDateTime = String.format("%d-%02d-%02d %02d:%02d", selectedYear, selectedMonth + 1, selectedDay, selectedHour, selectedMinute)
                etDate.setText(formattedDateTime)
            }, hour, minute, true).show()
        }, year, month, day).show()
    }
}