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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GroupDetailsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvName: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvCreator: TextView
    private lateinit var ivGroupImage: ImageView

    // Member List
    private lateinit var recyclerMembers: RecyclerView

    // --- NEW: Schedule List ---
    private lateinit var recyclerSchedules: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var scheduleList = ArrayList<Schedule>()

    // Buttons
    private lateinit var btnAddSchedule: Button
    private lateinit var btnLeave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: ImageView
    private lateinit var btnChat: ImageView

    private var groupId: Int = -1
    private var groupName: String = ""
    private var groupCode: String = ""
    private lateinit var currentUser: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

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

        // --- NEW: Initialize Schedule RecyclerView ---
        recyclerSchedules = view.findViewById(R.id.recyclerGroupSchedules) // Must match XML ID
        btnAddSchedule = view.findViewById(R.id.btnAddGroupSchedule)
        btnLeave = view.findViewById(R.id.btnLeaveGroup)
        btnDelete = view.findViewById(R.id.btnDeleteGroup)
        btnBack = view.findViewById(R.id.btnBackGroupDetail)
        btnChat = view.findViewById(R.id.btnGroupChat)

        tvName.text = groupName
        tvCode.text = groupCode

        // Load Group Image (Base64)
        loadGroupDetails()

        // --- NEW: Setup Schedule Adapter ---
        setupScheduleList()

        // Get Creator Info
        dbHelper.getGroupCreator(groupId) { creator ->
            tvCreator.text = "Created by: $creator"
            if (currentUser == creator) {
                btnDelete.visibility = View.VISIBLE
                btnLeave.visibility = View.GONE
            } else {
                btnDelete.visibility = View.GONE
                btnLeave.visibility = View.VISIBLE
            }
            setupMemberList(creator)
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnChat.setOnClickListener {
            val chatFragment = GroupChatFragment()
            val bundle = Bundle()
            bundle.putInt("GROUP_ID", groupId)
            bundle.putString("GROUP_NAME", groupName)
            chatFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, chatFragment)
                .addToBackStack(null)
                .commit()
        }

        btnAddSchedule.setOnClickListener {
            val fragment = AddSharedScheduleFragment()
            val bundle = Bundle()
            bundle.putInt("GROUP_ID", groupId)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        btnLeave.setOnClickListener {
            showConfirmationDialog("LEAVE GROUP?", "Are you sure you want to leave this group?") { leaveGroup() }
        }

        btnDelete.setOnClickListener {
            showConfirmationDialog("DELETE GROUP?", "This will remove the group and all its schedules. Are you sure?") { deleteGroup() }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Reload schedules when coming back from "AddSharedScheduleFragment"
        loadGroupSchedules()
    }

    private fun setupScheduleList() {
        recyclerSchedules.layoutManager = LinearLayoutManager(context)
        scheduleAdapter = ScheduleAdapter(scheduleList)
        recyclerSchedules.adapter = scheduleAdapter

        // Handle click on a schedule
        scheduleAdapter.setOnItemClickListener { schedule ->
            // Navigate to Schedule Details (Reuse your existing fragment)
            val detailFragment = ScheduleDetailFragment()
            val bundle = Bundle()
            bundle.putInt("SCHEDULE_ID", schedule.id)
            detailFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        // Initial Load
        loadGroupSchedules()
    }

    private fun loadGroupSchedules() {
        if (groupId == -1) return

        // Calls the new function in DatabaseHelper
        dbHelper.getGroupSchedules(groupId) { schedules ->
            scheduleList.clear()
            scheduleList.addAll(schedules)
            scheduleAdapter.notifyDataSetChanged()
        }
    }

    private fun loadGroupDetails() {
        dbHelper.getGroupDetails(groupId) { group ->
            if (group != null) {
                tvName.text = group.name
                val imageUrl = group.imageUrl
                if (imageUrl.isNotEmpty()) {
                    try {
                        val decodedString = Base64.decode(imageUrl, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        ivGroupImage.setImageBitmap(decodedByte)
                        ivGroupImage.setPadding(0, 0, 0, 0)
                        ivGroupImage.imageTintList = null
                    } catch (e: Exception) {
                        ivGroupImage.setImageResource(R.drawable.ic_group)
                    }
                }
            }
        }
    }

    private fun setupMemberList(creator: String) {
        dbHelper.getGroupMemberUsernames(groupId, "") { members ->
            recyclerMembers.layoutManager = LinearLayoutManager(context)
            recyclerMembers.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                    val tvUsername: TextView = view.findViewById(R.id.tvAttendeeName)
                    val tvStatus: TextView = view.findViewById(R.id.tvAttendeeStatus)
                    val ivAvatar: ImageView = view.findViewById(R.id.ivAttendeeAvatar)
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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

                        // Load Profile Pic (Base64 check handled in DbHelper or here)
                        dbHelper.getProfilePictureUrl(memberName) { url ->
                            // Note: If profile pics are also Base64, you should update this logic similarly
                            // But keeping your existing logic for now unless you say otherwise
                            if (url.isNotEmpty() && !url.startsWith("http")) {
                                try {
                                    val decodedString = Base64.decode(url, Base64.DEFAULT)
                                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                    holder.ivAvatar.setImageBitmap(decodedByte)
                                    holder.ivAvatar.colorFilter = null
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
                override fun getItemCount(): Int = members.size
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
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
        btnYes.setOnClickListener { onConfirm(); dialog.dismiss() }
        dialog.show()
    }

    private fun leaveGroup() {
        dbHelper.leaveGroup(groupId, currentUser) { success ->
            if (success) {
                Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun deleteGroup() {
        dbHelper.deleteGroup(groupId) { success ->
            if (success) {
                Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}