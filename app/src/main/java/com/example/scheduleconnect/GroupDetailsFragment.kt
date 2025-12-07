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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupDetailsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvName: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvCreator: TextView
    private lateinit var ivGroupImage: ImageView
    private lateinit var recyclerMembers: RecyclerView
    private lateinit var btnLeave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: ImageView

    private var groupId: Int = -1
    private var groupName: String = ""
    private var groupCode: String = ""
    private lateinit var currentUser: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate Main UI
        val view = inflater.inflate(R.layout.fragment_group_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Get Args
        groupId = arguments?.getInt("GROUP_ID") ?: -1
        groupName = arguments?.getString("GROUP_NAME") ?: "Unknown"
        groupCode = arguments?.getString("GROUP_CODE") ?: "N/A"

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // Initialize Views
        tvName = view.findViewById(R.id.tvDetailGroupName)
        tvCode = view.findViewById(R.id.tvDetailGroupCode)
        tvCreator = view.findViewById(R.id.tvDetailGroupCreator)
        ivGroupImage = view.findViewById(R.id.ivGroupDetailImage)
        recyclerMembers = view.findViewById(R.id.recyclerGroupMembers)

        btnLeave = view.findViewById(R.id.btnLeaveGroup)
        btnDelete = view.findViewById(R.id.btnDeleteGroup)
        btnBack = view.findViewById(R.id.btnBackGroupDetail)

        // Set Data
        tvName.text = groupName
        tvCode.text = groupCode
        val creator = dbHelper.getGroupCreator(groupId)
        tvCreator.text = "Created by: $creator"

        // Show/Hide buttons based on Admin status
        if (currentUser == creator) {
            btnDelete.visibility = View.VISIBLE
            btnLeave.visibility = View.GONE
        } else {
            btnDelete.visibility = View.GONE
            btnLeave.visibility = View.VISIBLE
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Setup Members List
        setupMemberList(creator)

        // Button Actions
        btnLeave.setOnClickListener {
            showConfirmationDialog("LEAVE GROUP?", "Are you sure you want to leave this group?") { leaveGroup() }
        }

        btnDelete.setOnClickListener {
            showConfirmationDialog("DELETE GROUP?", "This will remove the group and all its schedules. Are you sure?") { deleteGroup() }
        }

        return view
    }

    private fun setupMemberList(creator: String) {
        val members = dbHelper.getGroupMemberUsernames(groupId, "")

        recyclerMembers.layoutManager = LinearLayoutManager(context)
        recyclerMembers.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val tvUsername: TextView = view.findViewById(R.id.tvAttendeeName)
                val tvStatus: TextView = view.findViewById(R.id.tvAttendeeStatus)
                val ivAvatar: ImageView = view.findViewById(R.id.ivAttendeeAvatar)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                // IMPORTANT: Inflate item_attendee.xml
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
                return MemberViewHolder(v)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val memberName = members[position]
                if (holder is MemberViewHolder) {
                    holder.tvUsername.text = memberName

                    if (memberName == creator) {
                        holder.tvStatus.text = "Admin"
                        holder.tvStatus.setTextColor(Color.parseColor("#8B1A1A"))
                    } else {
                        holder.tvStatus.text = "Member"
                        holder.tvStatus.setTextColor(Color.GRAY)
                    }

                    // Attempt to load profile pic
                    val bmp = dbHelper.getProfilePicture(memberName)
                    if (bmp != null) {
                        holder.ivAvatar.setImageBitmap(bmp)
                        holder.ivAvatar.imageTintList = null
                    } else {
                        holder.ivAvatar.setImageResource(R.drawable.ic_person)
                        holder.ivAvatar.setColorFilter(Color.parseColor("#999999"))
                    }
                }
            }

            override fun getItemCount(): Int = members.size
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        // IMPORTANT: Inflate dialog_generic_confirmation.xml
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)

        tvTitle.text = title
        tvMessage.text = message

        btnNo.setOnClickListener { dialog.dismiss() }

        btnYes.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun leaveGroup() {
        val db = dbHelper.writableDatabase
        db.delete("group_members", "group_id=? AND username=?", arrayOf(groupId.toString(), currentUser))
        Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun deleteGroup() {
        val db = dbHelper.writableDatabase
        db.delete("schedule_groups", "group_id=?", arrayOf(groupId.toString()))
        db.delete("group_members", "group_id=?", arrayOf(groupId.toString()))
        db.delete("schedules", "group_id=?", arrayOf(groupId.toString()))

        Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }
}