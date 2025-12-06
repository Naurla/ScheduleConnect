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

        // 6. RSVP Button Actions
        view.findViewById<Button>(R.id.btnAttend).setOnClickListener {
            updateStatusAndShowDialog(1, "You are now marked as ATTENDING this schedule.")
        }
        view.findViewById<Button>(R.id.btnUnsure).setOnClickListener {
            updateStatusAndShowDialog(2, "Your status is set to UNSURE.")
        }
        view.findViewById<Button>(R.id.btnNotAttend).setOnClickListener {
            updateStatusAndShowDialog(3, "You are marked as NOT ATTENDING.")
        }

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

        // --- UPDATED: DELETE BUTTON LOGIC (Top Right Icon) ---
        // Changed type to ImageView
        val btnDelete = view.findViewById<ImageView>(R.id.btnDeleteSchedule)

        // Always visible for testing, or you can uncomment the restriction later
        btnDelete.visibility = View.VISIBLE

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        return view
    }

    private fun updateStatusAndShowDialog(status: Int, message: String) {
        dbHelper.updateRSVP(schId, currentUser, status)
        showCustomModal("Status Updated", message) {
            refreshStatusUI()
        }
    }

    private fun showCustomModal(title: String, message: String, onOkClick: () -> Unit) {
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
            onOkClick()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Schedule")
        builder.setMessage("Are you sure you want to delete this schedule? This action cannot be undone.")

        builder.setPositiveButton("DELETE") { dialog, _ ->
            val success = dbHelper.deleteSchedule(schId)
            if (success) {
                Toast.makeText(context, "Schedule Deleted Successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                parentFragmentManager.popBackStack() // Go back to previous list
            } else {
                Toast.makeText(context, "Failed to delete schedule", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
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
                1 -> {
                    btnCurrentStatus.text = "I WILL ATTEND"
                    btnCurrentStatus.background.setTint(Color.parseColor("#8B1A1A"))
                }
                2 -> {
                    btnCurrentStatus.text = "UNSURE"
                    btnCurrentStatus.background.setTint(Color.parseColor("#F57C00"))
                }
                3 -> {
                    btnCurrentStatus.text = "I WILL NOT ATTEND"
                    btnCurrentStatus.background.setTint(Color.parseColor("#555555"))
                }
            }
        }
    }
}