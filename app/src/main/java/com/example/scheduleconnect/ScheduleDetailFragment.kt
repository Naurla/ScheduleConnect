package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ScheduleDetailFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutButtons: LinearLayout
    private lateinit var layoutChangeMind: LinearLayout
    private lateinit var btnCurrentStatus: Button
    private lateinit var currentUser: String
    private var schId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Retrieve Arguments
        schId = arguments?.getInt("SCH_ID") ?: -1
        val title = arguments?.getString("SCH_TITLE")
        val date = arguments?.getString("SCH_DATE")
        val loc = arguments?.getString("SCH_LOC")
        val desc = arguments?.getString("SCH_DESC")
        val creator = arguments?.getString("SCH_CREATOR") ?: "Unknown"

        // 2. Bind Data to Views
        view.findViewById<TextView>(R.id.tvDetailTitle).text = title
        view.findViewById<TextView>(R.id.tvDetailDate).text = date
        view.findViewById<TextView>(R.id.tvDetailLoc).text = loc
        view.findViewById<TextView>(R.id.tvDetailDesc).text = desc
        view.findViewById<TextView>(R.id.tvDetailCreator).text = "Schedule by: $creator"

        // 3. Layout References
        layoutButtons = view.findViewById(R.id.layoutRSVPButtons)
        layoutChangeMind = view.findViewById(R.id.layoutChangeMind)
        btnCurrentStatus = view.findViewById(R.id.btnCurrentStatus)

        // 4. Get Current User
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // 5. Initialize View State
        refreshStatusUI()

        // 6. RSVP Button Actions with POPUP MODAL
        view.findViewById<Button>(R.id.btnAttend).setOnClickListener {
            updateStatusAndShowDialog(1, "You are now marked as ATTENDING this schedule.")
        }
        view.findViewById<Button>(R.id.btnUnsure).setOnClickListener {
            updateStatusAndShowDialog(2, "Your status is set to UNSURE.")
        }
        view.findViewById<Button>(R.id.btnNotAttend).setOnClickListener {
            updateStatusAndShowDialog(3, "You are marked as NOT ATTENDING.")
        }

        // 7. "Change your mind?" Logic
        view.findViewById<TextView>(R.id.tvChangeMind).setOnClickListener {
            layoutChangeMind.visibility = View.GONE
            layoutButtons.visibility = View.VISIBLE
        }

        // 8. View Attendees Button Logic
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

        // Back Button
        view.findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun updateStatusAndShowDialog(status: Int, message: String) {
        // 1. Update Database
        dbHelper.updateRSVP(schId, currentUser, status)

        // 2. Show Modal
        AlertDialog.Builder(requireContext())
            .setTitle("Status Updated")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // 3. Refresh UI to show the big button AFTER dialog confirms
                refreshStatusUI()
            }
            .setCancelable(false)
            .show()
    }

    private fun refreshStatusUI() {
        val status = dbHelper.getUserRSVPStatus(schId, currentUser)

        if (status == 0) {
            // No vote yet -> Show 3 Buttons
            layoutButtons.visibility = View.VISIBLE
            layoutChangeMind.visibility = View.GONE
        } else {
            // Voted -> Show Big Status Button
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.VISIBLE

            when (status) {
                1 -> {
                    btnCurrentStatus.text = "I WILL ATTEND"
                    btnCurrentStatus.background.setTint(android.graphics.Color.parseColor("#8B1A1A")) // Red
                }
                2 -> {
                    btnCurrentStatus.text = "UNSURE"
                    btnCurrentStatus.background.setTint(android.graphics.Color.parseColor("#F57C00")) // Orange
                }
                3 -> {
                    btnCurrentStatus.text = "I WILL NOT ATTEND"
                    btnCurrentStatus.background.setTint(android.graphics.Color.parseColor("#555555")) // Gray
                }
            }
        }
    }
}