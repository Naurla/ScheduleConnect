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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide // Ensure Glide is imported
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleDetailFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutButtons: LinearLayout
    private lateinit var layoutChangeMind: LinearLayout
    private lateinit var btnCurrentStatus: Button
    private lateinit var btnDelete: ImageView
    private lateinit var btnEdit: ImageView

    private lateinit var btnCancelPersonal: Button
    private lateinit var btnFinishSchedule: Button

    private lateinit var btnViewAttendees: Button

    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvLoc: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvCreator: TextView
    private lateinit var ivImage: ImageView // Added reference for Image

    private lateinit var currentUser: String
    private var schId: Int = -1
    private var groupId: Int = -1
    private var creator: String = ""
    private var type: String = ""
    private var currentStatus: String = "ACTIVE"
    private var isFromHistory: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        schId = arguments?.getInt("SCH_ID") ?: -1

        // Note: We will fetch these details asynchronously to ensure freshness,
        // but initial arguments are good for placeholders.
        creator = arguments?.getString("SCH_CREATOR") ?: "Unknown"
        type = arguments?.getString("SCH_TYPE") ?: "shared"
        isFromHistory = arguments?.getBoolean("IS_FROM_HISTORY", false) ?: false

        tvTitle = view.findViewById(R.id.tvDetailTitle)
        tvDate = view.findViewById(R.id.tvDetailDate)
        tvLoc = view.findViewById(R.id.tvDetailLoc)
        tvDesc = view.findViewById(R.id.tvDetailDesc)
        tvCreator = view.findViewById(R.id.tvDetailCreator)
        ivImage = view.findViewById(R.id.ivDetailImage) // Ensure this ID exists in XML

        layoutButtons = view.findViewById(R.id.layoutRSVPButtons)
        layoutChangeMind = view.findViewById(R.id.layoutChangeMind)
        btnCurrentStatus = view.findViewById(R.id.btnCurrentStatus)
        btnDelete = view.findViewById(R.id.btnDeleteSchedule)
        btnEdit = view.findViewById(R.id.btnEditSchedule)

        btnCancelPersonal = view.findViewById(R.id.btnCancelPersonal)
        btnFinishSchedule = view.findViewById(R.id.btnFinishSchedule)

        btnViewAttendees = view.findViewById(R.id.btnViewAttendees)

        view.findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        view.findViewById<Button>(R.id.btnAttend).setOnClickListener { updateRSVP(1, "You are ATTENDING.") }
        view.findViewById<Button>(R.id.btnUnsure).setOnClickListener { updateRSVP(2, "Your status is UNSURE.") }
        view.findViewById<Button>(R.id.btnNotAttend).setOnClickListener { updateRSVP(3, "You are NOT ATTENDING.") }

        view.findViewById<TextView>(R.id.tvChangeMind).setOnClickListener {
            layoutChangeMind.visibility = View.GONE
            layoutButtons.visibility = View.VISIBLE
        }

        btnViewAttendees.setOnClickListener {
            val fragment = AttendeeListFragment()
            val bundle = Bundle()
            bundle.putInt("SCH_ID", schId)
            bundle.putString("SCH_TITLE", tvTitle.text.toString())
            bundle.putString("SCH_CREATOR", creator)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        btnEdit.setOnClickListener {
            val fragment = EditScheduleFragment()
            val bundle = Bundle()
            bundle.putInt("SCH_ID", schId)
            // Passing data is okay, but EditFragment will also re-fetch to be safe
            bundle.putString("SCH_TITLE", tvTitle.text.toString())
            bundle.putString("SCH_DATE", tvDate.text.toString())
            bundle.putString("SCH_LOC", tvLoc.text.toString())
            bundle.putString("SCH_DESC", tvDesc.text.toString())
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        btnFinishSchedule.setOnClickListener { updateScheduleStatus("FINISHED") }

        btnCancelPersonal.setOnClickListener { showCancelConfirmation() }

        btnDelete.setOnClickListener { showDeleteConfirmation() }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadScheduleDetails()
    }

    private fun loadScheduleDetails() {
        // --- ASYNC FETCH ---
        dbHelper.getSchedule(schId) { schedule ->
            if (schedule != null) {
                tvTitle.text = schedule.title
                tvDate.text = schedule.date
                tvLoc.text = schedule.location

                if (schedule.description.trim().isEmpty()) {
                    tvDesc.text = "No description provided."
                    tvDesc.setTextColor(Color.parseColor("#999999"))
                    tvDesc.setTypeface(null, android.graphics.Typeface.ITALIC)
                } else {
                    tvDesc.text = schedule.description
                    tvDesc.setTextColor(Color.parseColor("#555555"))
                    tvDesc.setTypeface(null, android.graphics.Typeface.NORMAL)
                }

                creator = schedule.creator
                tvCreator.text = "Schedule by: $creator"
                currentStatus = schedule.status
                groupId = schedule.groupId
                type = schedule.type // Ensure type is updated from DB

                // --- GLIDE IMAGE LOADING ---
                if (schedule.imageUrl.isNotEmpty()) {
                    ivImage.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(schedule.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivImage)
                } else {
                    ivImage.visibility = View.GONE
                }

                updateButtonsVisibility()

                // Refresh RSVP UI if user is not creator
                if (type == "shared" && currentUser != creator) {
                    refreshStatusUI()
                }
            } else {
                Toast.makeText(context, "Error loading schedule", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun updateButtonsVisibility() {
        if (isFromHistory || currentStatus == "FINISHED" || currentStatus == "CANCELLED") {
            btnFinishSchedule.visibility = View.GONE
            btnCancelPersonal.visibility = View.GONE
            btnDelete.visibility = View.GONE
            btnEdit.visibility = View.GONE
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.GONE

            if (type == "shared" && currentUser == creator) {
                btnViewAttendees.visibility = View.VISIBLE
            } else {
                btnViewAttendees.visibility = View.GONE
            }
            return
        }

        if (type == "personal") {
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.GONE
            btnViewAttendees.visibility = View.GONE

            btnDelete.visibility = View.VISIBLE
            btnFinishSchedule.visibility = View.VISIBLE
            btnCancelPersonal.visibility = View.VISIBLE
            btnEdit.visibility = View.VISIBLE

        } else {
            if (currentUser == creator) {
                btnDelete.visibility = View.VISIBLE
                btnEdit.visibility = View.VISIBLE
                btnFinishSchedule.visibility = View.VISIBLE
                btnViewAttendees.visibility = View.VISIBLE

                layoutButtons.visibility = View.GONE
                layoutChangeMind.visibility = View.GONE
                btnCancelPersonal.visibility = View.GONE
            } else {
                btnDelete.visibility = View.GONE
                btnEdit.visibility = View.GONE
                btnFinishSchedule.visibility = View.GONE
                btnCancelPersonal.visibility = View.GONE
                btnViewAttendees.visibility = View.GONE
            }
        }
    }

    private fun showCancelConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_cancel_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnNo = view.findViewById<TextView>(R.id.btnModalNo)
        val btnYes = view.findViewById<Button>(R.id.btnModalYes)

        btnNo.setOnClickListener { dialog.dismiss() }

        btnYes.setOnClickListener {
            updateScheduleStatus("CANCELLED")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateScheduleStatus(status: String) {
        dbHelper.updateScheduleStatus(schId, status) { success ->
            if (success) {
                Toast.makeText(context, "Schedule marked as $status", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Error updating status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDeleteTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDeleteMessage)
        val etReason = view.findViewById<EditText>(R.id.etDeleteReason)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmDelete)

        if (type == "shared" && currentUser == creator) {
            tvTitle.text = "Delete Shared Schedule"
            tvMessage.text = "This will notify all members. Please provide a reason."
            etReason.visibility = View.VISIBLE
        } else {
            tvTitle.text = "Delete Schedule"
            tvMessage.text = "Are you sure you want to delete this schedule?"
            etReason.visibility = View.GONE
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            if (type == "shared" && currentUser == creator) {
                val reason = etReason.text.toString().trim()
                val finalReason = if (reason.isNotEmpty()) reason else "No reason provided."

                dbHelper.deleteSchedule(schId) { success ->
                    if(success) {
                        notifyAttendeesOfDeletion(finalReason)
                        Toast.makeText(context, "Schedule Deleted", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        parentFragmentManager.popBackStack()
                    }
                }
            } else {
                dbHelper.deleteSchedule(schId) { success ->
                    if(success) {
                        Toast.makeText(context, "Schedule Deleted", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun notifyAttendeesOfDeletion(reason: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val title = tvTitle.text.toString()
        val notifTitle = "Schedule Deleted: $title"
        val notifMessage = "Deleted by $creator. Reason: $reason"

        dbHelper.getGroupMemberUsernames(groupId, creator) { members ->
            for (user in members) {
                dbHelper.addNotification(user, notifTitle, notifMessage, currentDate)
            }
        }

        // Email logic would be asynchronous here too if fully implemented
    }

    private fun updateRSVP(status: Int, msg: String) {
        dbHelper.updateRSVP(schId, currentUser, status)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        refreshStatusUI()
    }

    private fun refreshStatusUI() {
        // --- ASYNC RSVP CHECK ---
        dbHelper.getUserRSVPStatus(schId, currentUser) { status ->
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
}