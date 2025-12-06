package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ScheduleDetailFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutButtons: LinearLayout
    private lateinit var layoutChangeMind: LinearLayout
    private lateinit var btnCurrentStatus: Button
    private lateinit var btnDelete: ImageView
    private lateinit var btnCancelPersonal: Button
    private lateinit var btnViewAttendees: Button

    private lateinit var currentUser: String
    private var schId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Get Arguments
        schId = arguments?.getInt("SCH_ID") ?: -1
        val title = arguments?.getString("SCH_TITLE")
        val date = arguments?.getString("SCH_DATE")
        val loc = arguments?.getString("SCH_LOC")
        val desc = arguments?.getString("SCH_DESC")
        val creator = arguments?.getString("SCH_CREATOR") ?: "Unknown"
        val type = arguments?.getString("SCH_TYPE") ?: "shared" // Default to shared

        // 2. Bind Views
        view.findViewById<TextView>(R.id.tvDetailTitle).text = title
        view.findViewById<TextView>(R.id.tvDetailDate).text = date
        view.findViewById<TextView>(R.id.tvDetailLoc).text = loc
        view.findViewById<TextView>(R.id.tvDetailDesc).text = desc
        view.findViewById<TextView>(R.id.tvDetailCreator).text = "Schedule by: $creator"

        layoutButtons = view.findViewById(R.id.layoutRSVPButtons)
        layoutChangeMind = view.findViewById(R.id.layoutChangeMind)
        btnCurrentStatus = view.findViewById(R.id.btnCurrentStatus)
        btnDelete = view.findViewById(R.id.btnDeleteSchedule)
        btnCancelPersonal = view.findViewById(R.id.btnCancelPersonal)
        btnViewAttendees = view.findViewById(R.id.btnViewAttendees)

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // 3. UI LOGIC: Personal vs Shared
        if (type == "personal") {
            // --- PERSONAL VIEW ---
            // Hide RSVP / Attendee / Trash Icon
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.GONE
            btnViewAttendees.visibility = View.GONE
            btnDelete.visibility = View.GONE

            // Show Cancel Button
            btnCancelPersonal.visibility = View.VISIBLE
            btnCancelPersonal.setOnClickListener {
                showCancelConfirmation()
            }

        } else {
            // --- SHARED VIEW ---
            btnCancelPersonal.visibility = View.GONE
            btnViewAttendees.visibility = View.VISIBLE

            // Show Delete icon ONLY if user is creator
            if (currentUser == creator) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { showDeleteConfirmation() }
            } else {
                btnDelete.visibility = View.GONE
            }

            // RSVP Status Logic
            refreshStatusUI()
        }

        // 4. RSVP Button Listeners (Only active for Shared)
        view.findViewById<Button>(R.id.btnAttend).setOnClickListener { updateStatus(1, "You are ATTENDING.") }
        view.findViewById<Button>(R.id.btnUnsure).setOnClickListener { updateStatus(2, "Your status is UNSURE.") }
        view.findViewById<Button>(R.id.btnNotAttend).setOnClickListener { updateStatus(3, "You are NOT ATTENDING.") }

        view.findViewById<TextView>(R.id.tvChangeMind).setOnClickListener {
            layoutChangeMind.visibility = View.GONE
            layoutButtons.visibility = View.VISIBLE
        }

        view.findViewById<Button>(R.id.btnViewAttendees).setOnClickListener {
            val fragment = AttendeeListFragment()
            val bundle = Bundle()
            bundle.putInt("SCH_ID", schId)
            bundle.putString("SCH_TITLE", title)
            bundle.putString("SCH_CREATOR", creator)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    // Handles logic for "Cancel Schedule" (Personal)
    private fun showCancelConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Schedule")
            .setMessage("Are you sure you want to cancel this schedule? It will be removed from your list.")
            .setPositiveButton("YES, CANCEL") { _, _ ->
                if (dbHelper.deleteSchedule(schId)) {
                    Toast.makeText(context, "Schedule Cancelled", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Error cancelling", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("NO", null)
            .show()
    }

    // Handles logic for "Delete Schedule" (Shared Creator)
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Group Schedule")
            .setMessage("Are you sure? This will remove it for EVERYONE in the group.")
            .setPositiveButton("Delete") { _, _ ->
                if (dbHelper.deleteSchedule(schId)) {
                    Toast.makeText(context, "Schedule Deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatus(status: Int, msg: String) {
        dbHelper.updateRSVP(schId, currentUser, status)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        refreshStatusUI()
    }

    private fun refreshStatusUI() {
        val status = dbHelper.getUserRSVPStatus(schId, currentUser)
        if (status == 0) {
            layoutButtons.visibility = View.VISIBLE
            layoutChangeMind.visibility = View.GONE
        } else {
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.VISIBLE
            when (status) {
                1 -> { btnCurrentStatus.text = "I WILL ATTEND"; btnCurrentStatus.background.setTint(Color.parseColor("#8B1A1A")) }
                2 -> { btnCurrentStatus.text = "UNSURE"; btnCurrentStatus.background.setTint(Color.parseColor("#F57C00")) }
                3 -> { btnCurrentStatus.text = "I WILL NOT ATTEND"; btnCurrentStatus.background.setTint(Color.parseColor("#555555")) }
            }
        }
    }
}