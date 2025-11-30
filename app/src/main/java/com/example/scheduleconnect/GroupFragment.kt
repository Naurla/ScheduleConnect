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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class GroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var currentUser: String
    private lateinit var currentGroupCode: String

    // NEW: Variable to store the user's real name (e.g., "James")
    private var currentUserName: String = ""

    // Views for Group List (Screen 4)
    private lateinit var cardGroupItem: androidx.cardview.widget.CardView
    private lateinit var tvGroupNameList: TextView

    // Views for Group Details (Screen 5)
    private lateinit var tvDetailGroupName: TextView
    private lateinit var tvDetailGroupCode: TextView
    private lateinit var tvNoSharedSchedules: TextView
    private lateinit var recyclerSharedSchedules: RecyclerView
    private lateinit var btnAddSharedSchedule: Button

    // Views for Add Shared Form (Screen 6)
    private lateinit var etSharedName: EditText
    private lateinit var etSharedDate: EditText
    private lateinit var etSharedLocation: EditText
    private lateinit var etSharedDesc: EditText
    private lateinit var btnAddSharedConfirm: Button
    private lateinit var btnCancelShared: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Get Current Username from Session
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // 2. Fetch the Real Name (e.g., "James") for the "Created By" feature
        // Note: Ensure your DatabaseHelper has the getUserName() method.
        currentUserName = dbHelper.getUserName(currentUser) ?: currentUser

        viewFlipper = view.findViewById(R.id.viewFlipperGroup)

        // 3. Initialize all UI components
        initNavigationViews(view)
        initGroupListViews(view)
        initGroupDetailsViews(view)
        initAddFormViews(view)

        // 4. Check if user is already in a group
        val existingGroupCode = dbHelper.getUserGroupCode(currentUser)
        if (existingGroupCode != null) {
            currentGroupCode = existingGroupCode
            loadGroupListUI() // Go to List (Screen 4)
        } else {
            viewFlipper.displayedChild = 0 // Go to Start (Screen 0)
        }

        return view
    }

    private fun initNavigationViews(view: View) {
        // Navigation Buttons
        view.findViewById<Button>(R.id.btnStart).setOnClickListener { viewFlipper.displayedChild = 1 }
        view.findViewById<Button>(R.id.btnGoToCreate).setOnClickListener { viewFlipper.displayedChild = 2 }
        view.findViewById<Button>(R.id.btnGoToJoin).setOnClickListener { viewFlipper.displayedChild = 3 }

        // Back Buttons
        view.findViewById<ImageButton>(R.id.btnBackChoose).setOnClickListener { viewFlipper.displayedChild = 0 }
        view.findViewById<ImageButton>(R.id.btnBackCreate).setOnClickListener { viewFlipper.displayedChild = 1 }
        view.findViewById<ImageButton>(R.id.btnBackJoin).setOnClickListener { viewFlipper.displayedChild = 1 }

        // Logic: Create Group
        val etGroupName = view.findViewById<EditText>(R.id.etGroupName)
        view.findViewById<Button>(R.id.btnCreateConfirm).setOnClickListener {
            if (etGroupName.text.toString().isNotEmpty()) {
                val code = dbHelper.createGroup(etGroupName.text.toString(), currentUser)
                if (code != null) {
                    currentGroupCode = code
                    loadGroupListUI()
                } else {
                    Toast.makeText(context, "Error creating group", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Enter Group Name", Toast.LENGTH_SHORT).show()
            }
        }

        // Logic: Join Group
        val etGroupCode = view.findViewById<EditText>(R.id.etGroupCode)
        view.findViewById<Button>(R.id.btnJoinConfirm).setOnClickListener {
            val code = etGroupCode.text.toString().trim()
            val result = dbHelper.joinGroup(code, currentUser)
            if (result == 0) {
                currentGroupCode = code
                loadGroupListUI()
            } else if (result == 1) {
                Toast.makeText(context, "Group not found", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "User error. Re-login.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initGroupListViews(view: View) {
        cardGroupItem = view.findViewById(R.id.cardGroupItem)
        tvGroupNameList = view.findViewById(R.id.tvGroupNameList)

        // Click card -> Go to Details
        cardGroupItem.setOnClickListener {
            loadGroupDetailsUI()
        }
    }

    private fun initGroupDetailsViews(view: View) {
        tvDetailGroupName = view.findViewById(R.id.tvDetailGroupName)
        tvDetailGroupCode = view.findViewById(R.id.tvDetailGroupCode)
        tvNoSharedSchedules = view.findViewById(R.id.tvNoSharedSchedules)
        recyclerSharedSchedules = view.findViewById(R.id.recyclerSharedSchedules)
        btnAddSharedSchedule = view.findViewById(R.id.btnAddSharedSchedule)

        recyclerSharedSchedules.layoutManager = LinearLayoutManager(context)

        // Click Add -> Go to Form (Screen 6)
        btnAddSharedSchedule.setOnClickListener {
            etSharedName.text.clear()
            etSharedDate.text.clear()
            etSharedLocation.text.clear()
            etSharedDesc.text.clear()
            viewFlipper.displayedChild = 6
        }
    }

    private fun initAddFormViews(view: View) {
        etSharedName = view.findViewById(R.id.etSharedName)
        etSharedDate = view.findViewById(R.id.etSharedDate)
        etSharedLocation = view.findViewById(R.id.etSharedLocation)
        etSharedDesc = view.findViewById(R.id.etSharedDesc)
        btnAddSharedConfirm = view.findViewById(R.id.btnAddSharedConfirm)
        btnCancelShared = view.findViewById(R.id.btnCancelShared)

        // 1. Date Picker
        etSharedDate.setOnClickListener {
            showDateTimePicker()
        }

        // 2. Cancel Action
        btnCancelShared.setOnClickListener {
            viewFlipper.displayedChild = 5 // Back to Details
        }

        // 3. Confirm Add Action
        btnAddSharedConfirm.setOnClickListener {
            val name = etSharedName.text.toString()
            val date = etSharedDate.text.toString()
            val loc = etSharedLocation.text.toString()
            val desc = etSharedDesc.text.toString()

            if (name.isNotEmpty() && date.isNotEmpty()) {
                // UPDATED: Pass 'currentUserName' as the creator
                val success = dbHelper.addSchedule(currentUser, name, date, loc, desc, "shared", currentUserName)

                if (success) {
                    Toast.makeText(context, "Shared Schedule Added!", Toast.LENGTH_SHORT).show()
                    loadGroupDetailsUI() // Refresh and go back
                } else {
                    Toast.makeText(context, "Error adding schedule", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Name and Date are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val selectedDateTime = Calendar.getInstance()
                selectedDateTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)

                val format = SimpleDateFormat("EEE, MMM d, yyyy h:mma", Locale.getDefault())
                etSharedDate.setText(format.format(selectedDateTime.time))

            }, hour, minute, false)
            timePickerDialog.show()

        }, year, month, day)
        datePickerDialog.show()
    }

    private fun loadGroupListUI() {
        val cursor = dbHelper.getGroupInfo(currentGroupCode)
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.GRP_NAME))
            tvGroupNameList.text = name.uppercase()
            cursor.close()
        }
        viewFlipper.displayedChild = 4 // Show Group List Screen
    }

    private fun loadGroupDetailsUI() {
        // Load Group Info
        val cursor = dbHelper.getGroupInfo(currentGroupCode)
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.GRP_NAME))
            tvDetailGroupName.text = name.uppercase()
            tvDetailGroupCode.text = "SHARED SCHEDULE CODE: $currentGroupCode"
            cursor.close()
        }

        // Load Shared Schedules (including creator info)
        val sharedSchedules = dbHelper.getSchedules(currentUser, "shared")

        if (sharedSchedules.isEmpty()) {
            tvNoSharedSchedules.visibility = View.VISIBLE
            recyclerSharedSchedules.visibility = View.GONE
        } else {
            tvNoSharedSchedules.visibility = View.GONE
            recyclerSharedSchedules.visibility = View.VISIBLE
            recyclerSharedSchedules.adapter = ScheduleAdapter(sharedSchedules)
        }

        viewFlipper.displayedChild = 5 // Show Group Details Screen
    }
}