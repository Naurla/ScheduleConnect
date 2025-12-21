package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
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
    private lateinit var ivImage: ImageView

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

        schId = arguments?.getInt("SCHEDULE_ID", -1) ?: -1
        if (schId == -1) {
            schId = arguments?.getInt("SCH_ID", -1) ?: -1
        }

        creator = arguments?.getString("SCH_CREATOR") ?: "Unknown"
        type = arguments?.getString("SCH_TYPE") ?: "shared"
        isFromHistory = arguments?.getBoolean("IS_FROM_HISTORY", false) ?: false

        tvTitle = view.findViewById(R.id.tvDetailTitle)
        tvDate = view.findViewById(R.id.tvDetailDate)
        tvLoc = view.findViewById(R.id.tvDetailLoc)
        tvDesc = view.findViewById(R.id.tvDetailDesc)
        tvCreator = view.findViewById(R.id.tvDetailCreator)
        ivImage = view.findViewById(R.id.ivDetailImage)

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
        currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

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

        btnFinishSchedule.setOnClickListener { showFinishConfirmation() }

        btnCancelPersonal.setOnClickListener {
            if (type == "shared") {
                showTerminalDialog(isDeletion = false)
            } else {
                showCancelConfirmation()
            }
        }

        btnDelete.setOnClickListener {
            showTerminalDialog(isDeletion = true)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (schId != -1) {
            loadScheduleDetails()
        }
    }

    private fun loadScheduleDetails() {
        dbHelper.getSchedule(schId) { schedule ->
            if (schedule != null) {
                tvTitle.text = schedule.title
                tvDate.text = schedule.date
                tvLoc.text = schedule.location
                tvDesc.text = if (schedule.description.trim().isEmpty()) "No description provided." else schedule.description

                creator = schedule.creator
                if (schedule.type == "shared") {
                    tvCreator.text = "Created by: $creator"
                    tvCreator.visibility = View.VISIBLE
                } else {
                    tvCreator.visibility = View.GONE
                }

                currentStatus = schedule.status
                groupId = schedule.groupId
                type = schedule.type

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                try {
                    val scheduleDate = dateFormat.parse(schedule.date)
                    if (scheduleDate != null && scheduleDate.before(Date())) {
                        isFromHistory = true
                    }
                } catch (e: Exception) { e.printStackTrace() }

                if (currentStatus == "FINISHED" || currentStatus == "CANCELLED") {
                    isFromHistory = true
                }

                if (schedule.imageUrl.isNotEmpty()) {
                    try {
                        val decodedString = Base64.decode(schedule.imageUrl, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        ivImage.setImageBitmap(decodedByte)
                        ivImage.visibility = View.VISIBLE
                    } catch (e: Exception) { ivImage.visibility = View.GONE }
                } else {
                    ivImage.visibility = View.GONE
                }
                updateButtonsVisibility()
                if (type == "shared" && currentUser != creator) {
                    refreshStatusUI()
                }
            }
        }
    }

    private fun updateButtonsVisibility() {
        if (currentStatus == "FINISHED" || currentStatus == "CANCELLED") {
            btnFinishSchedule.visibility = View.GONE
            btnCancelPersonal.visibility = View.GONE
            btnDelete.visibility = View.GONE
            btnEdit.visibility = View.GONE
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.GONE
            btnViewAttendees.visibility = if (type == "shared") View.VISIBLE else View.GONE
            return
        }

        if (type == "personal") {
            btnDelete.visibility = View.VISIBLE
            btnFinishSchedule.visibility = View.VISIBLE
            btnCancelPersonal.visibility = View.VISIBLE
            btnEdit.visibility = View.VISIBLE
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.GONE
        } else {
            btnViewAttendees.visibility = View.VISIBLE
            // Show delete icon for shared schedule ONLY if the user is the owner
            if (currentUser == creator) {
                btnDelete.visibility = View.VISIBLE
                btnEdit.visibility = View.VISIBLE
                btnFinishSchedule.visibility = View.VISIBLE
                btnCancelPersonal.visibility = View.VISIBLE
            } else {
                btnDelete.visibility = View.GONE
                btnEdit.visibility = View.GONE
                btnFinishSchedule.visibility = View.GONE
                btnCancelPersonal.visibility = View.GONE
            }
        }
    }

    private fun showFinishConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.tvDialogTitle).text = "Finish Schedule"
        view.findViewById<TextView>(R.id.tvDialogMessage).text = "Are you sure you want to mark this schedule as finished?"

        view.findViewById<TextView>(R.id.btnDialogNo).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnDialogYes).setOnClickListener {
            updateScheduleStatus("FINISHED")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showCancelConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_cancel_confirmation, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        view.findViewById<TextView>(R.id.btnModalNo).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnModalYes).setOnClickListener {
            updateScheduleStatus("CANCELLED")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showTerminalDialog(isDeletion: Boolean) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitleDialog = view.findViewById<TextView>(R.id.tvDeleteTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDeleteMessage)
        val etReason = view.findViewById<EditText>(R.id.etDeleteReason)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmDelete)

        if (isDeletion) {
            tvTitleDialog.text = "Delete Schedule"
            tvMessage.text = "Are you sure? This will permanently remove the schedule and it will NOT appear in history."
            btnConfirm.text = "DELETE PERMANENTLY"
            // reason is optional for personal, but we'll show it for shared deletion
            etReason.visibility = if (type == "shared") View.VISIBLE else View.GONE
        } else {
            tvTitleDialog.text = "Cancel Shared Schedule"
            tvMessage.text = "This will move the schedule to History. Please provide a reason."
            btnConfirm.text = "CANCEL TO HISTORY"
            etReason.visibility = View.VISIBLE
        }

        view.findViewById<TextView>(R.id.btnCancelDelete).setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val reason = etReason.text.toString().trim()
            if (type == "shared" && reason.isEmpty()) {
                Toast.makeText(context, "Please provide a reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val finalReason = if (reason.isNotEmpty()) reason else "No reason provided."

            if (isDeletion) {
                dbHelper.deleteSchedule(schId) { success ->
                    if (success) {
                        if (type == "shared") notifyAttendeesOfAction("Deleted", finalReason)
                        Toast.makeText(context, "Schedule Deleted Permanently", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                        dialog.dismiss()
                    }
                }
            } else {
                dbHelper.updateScheduleStatus(schId, "CANCELLED") { success ->
                    if (success) {
                        notifyAttendeesOfAction("Cancelled", finalReason)
                        Toast.makeText(context, "Schedule Moved to History", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun notifyAttendeesOfAction(action: String, reason: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val notifTitle = "Schedule $action: ${tvTitle.text}"
        val notifMessage = "$action by $creator. Reason: $reason"

        dbHelper.getGroupMemberUsernames(groupId, creator) { members ->
            for (user in members) {
                dbHelper.addNotification(user, notifTitle, notifMessage, currentDate)
            }
        }
    }

    private fun updateScheduleStatus(status: String) {
        dbHelper.updateScheduleStatus(schId, status) { success ->
            if (success) {
                Toast.makeText(context, "Schedule marked as $status", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun updateRSVP(status: Int, msg: String) {
        dbHelper.updateRSVP(schId, currentUser, status)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        refreshStatusUI()
    }

    private fun refreshStatusUI() {
        if (currentStatus == "FINISHED" || currentStatus == "CANCELLED" || isFromHistory) {
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.GONE
            return
        }

        dbHelper.getUserRSVPStatus(schId, currentUser) { status ->
            if (status == 0) {
                layoutButtons.visibility = View.VISIBLE
                layoutChangeMind.visibility = View.GONE
            } else {
                layoutButtons.visibility = View.GONE
                layoutChangeMind.visibility = View.VISIBLE
                when (status) {
                    1 -> btnCurrentStatus.text = "I WILL ATTEND"
                    2 -> btnCurrentStatus.text = "UNSURE"
                    3 -> btnCurrentStatus.text = "I WILL NOT ATTEND"
                }
            }
        }
    }
}