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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupDetailsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvName: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvCreator: TextView
    private lateinit var ivGroupImage: ImageView
    // REMOVED: btnSettings variable

    private lateinit var recyclerMembers: RecyclerView
    private lateinit var recyclerSchedules: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var scheduleList = ArrayList<Schedule>()

    private lateinit var btnAddSchedule: Button
    private lateinit var btnInvite: Button
    private lateinit var btnLeave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: ImageView
    private lateinit var btnChat: ImageView

    private var groupId: Int = -1
    private var currentGroupName: String = ""
    private var currentGroupNickname: String = ""
    private var groupCode: String = ""
    private lateinit var currentUser: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        groupId = arguments?.getInt("GROUP_ID") ?: -1
        currentGroupName = arguments?.getString("GROUP_NAME") ?: "Unknown"
        groupCode = arguments?.getString("GROUP_CODE") ?: "N/A"

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        // Initialize Views
        tvName = view.findViewById(R.id.tvDetailGroupName)
        tvCode = view.findViewById(R.id.tvDetailGroupCode)
        tvCreator = view.findViewById(R.id.tvDetailGroupCreator)
        ivGroupImage = view.findViewById(R.id.ivGroupDetailImage)

        // REMOVED: btnSettings = view.findViewById(R.id.btnGroupSettings)

        recyclerMembers = view.findViewById(R.id.recyclerGroupMembers)
        recyclerSchedules = view.findViewById(R.id.recyclerGroupSchedules)
        btnAddSchedule = view.findViewById(R.id.btnAddGroupSchedule)
        btnInvite = view.findViewById(R.id.btnInviteMember)
        btnLeave = view.findViewById(R.id.btnLeaveGroup)
        btnDelete = view.findViewById(R.id.btnDeleteGroup)
        btnBack = view.findViewById(R.id.btnBackGroupDetail)
        btnChat = view.findViewById(R.id.btnGroupChat)

        tvName.text = currentGroupName
        tvCode.text = groupCode

        recyclerMembers.layoutManager = LinearLayoutManager(context)
        recyclerMembers.isNestedScrollingEnabled = false

        recyclerSchedules.layoutManager = LinearLayoutManager(context)
        recyclerSchedules.isNestedScrollingEnabled = false

        loadGroupDetails()
        setupScheduleList()

        // Get Creator Info
        dbHelper.getGroupCreator(groupId) { creator ->
            val displayCreator = if (creator == "Unknown" || creator.isEmpty()) "Admin" else creator
            tvCreator.text = "Created by: $displayCreator"

            if (currentUser == creator) {
                // Admin View
                btnDelete.visibility = View.VISIBLE
                btnLeave.visibility = View.GONE
                btnInvite.visibility = View.VISIBLE
                // REMOVED: btnSettings.visibility = View.VISIBLE
            } else {
                // Member View
                btnDelete.visibility = View.GONE
                btnLeave.visibility = View.VISIBLE
                btnInvite.visibility = View.GONE
                // REMOVED: btnSettings.visibility = View.GONE
            }
            setupMemberList(creator)
        }

        // REMOVED: btnSettings.setOnClickListener listener

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnChat.setOnClickListener {
            val chatFragment = GroupChatFragment()
            val bundle = Bundle()
            bundle.putInt("GROUP_ID", groupId)
            val displayName = if (currentGroupNickname.isNotEmpty()) currentGroupNickname else currentGroupName
            bundle.putString("GROUP_NAME", displayName)
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

        btnInvite.setOnClickListener {
            showInviteDialog()
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
        loadGroupDetails()
        loadGroupSchedules()
    }

    private fun setupScheduleList() {
        scheduleAdapter = ScheduleAdapter(scheduleList)
        recyclerSchedules.adapter = scheduleAdapter

        scheduleAdapter.setOnItemClickListener { schedule ->
            val detailFragment = ScheduleDetailFragment()
            val bundle = Bundle()
            bundle.putInt("SCH_ID", schedule.id)
            bundle.putString("SCH_TITLE", schedule.title)
            bundle.putString("SCH_DATE", schedule.date)
            bundle.putString("SCH_LOC", schedule.location)
            bundle.putString("SCH_DESC", schedule.description)
            bundle.putString("SCH_CREATOR", schedule.creator)
            bundle.putString("SCH_TYPE", schedule.type)
            bundle.putString("SCH_IMAGE", schedule.imageUrl)
            detailFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        loadGroupSchedules()
    }

    private fun loadGroupSchedules() {
        if (groupId == -1) return
        dbHelper.getGroupSchedules(groupId) { schedules ->
            scheduleList.clear()
            scheduleList.addAll(schedules)
            scheduleAdapter.notifyDataSetChanged()
        }
    }

    private fun loadGroupDetails() {
        dbHelper.getGroupDetails(groupId) { group ->
            if (group != null) {
                currentGroupName = group.name
                currentGroupNickname = group.nickname
                groupCode = group.code
                val imageUrl = group.imageUrl

                if (currentGroupNickname.isNotEmpty()) {
                    tvName.text = currentGroupNickname
                } else {
                    tvName.text = currentGroupName
                }

                if (group.code.isNotEmpty()) {
                    tvCode.text = group.code
                }

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
            recyclerMembers.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                    val tvUsername: TextView = view.findViewById(R.id.tvAttendeeName)
                    val tvStatus: TextView = view.findViewById(R.id.tvAttendeeStatus)
                    val ivAvatar: ImageView = view.findViewById(R.id.ivAttendeeAvatar)
                    val btnKick: ImageView = view.findViewById(R.id.btnKickMember)

                    // ⚠️ FIX FOR UNRESOLVED REFERENCE ERROR:
                    // You MUST replace R.id.pencil_icon_id with the actual android:id
                    // of the pencil ImageView in your item_attendee.xml.
                    // If you are SURE R.id.ivEdit is the correct ID, uncomment this line.
                    val ivEdit: ImageView? = view.findViewById(resources.getIdentifier("ivEdit", "id", requireContext().packageName))
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
                    return MemberViewHolder(v)
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val memberName = members[position]
                    if (holder is MemberViewHolder) {
                        holder.tvUsername.text = memberName

                        // HIDE THE EDIT BUTTON
                        holder.ivEdit?.visibility = View.GONE

                        if (memberName == creator) {
                            holder.tvStatus.text = "Admin"
                            holder.tvStatus.setTextColor(Color.parseColor("#8B1A1A"))
                        } else {
                            holder.tvStatus.text = "Member"
                            holder.tvStatus.setTextColor(Color.GRAY)
                        }

                        if (currentUser == creator && memberName != creator) {
                            holder.btnKick.visibility = View.VISIBLE
                            holder.btnKick.setOnClickListener { showKickConfirmation(memberName) }
                        } else {
                            holder.btnKick.visibility = View.GONE
                        }

                        dbHelper.getProfilePictureUrl(memberName) { url ->
                            if (url.isNotEmpty()) {
                                try {
                                    val decodedString = Base64.decode(url, Base64.DEFAULT)
                                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                    holder.ivAvatar.setImageBitmap(decodedByte)
                                    holder.ivAvatar.imageTintList = null
                                    holder.ivAvatar.colorFilter = null
                                } catch (e: Exception) {
                                    holder.ivAvatar.setImageResource(R.drawable.ic_person)
                                    holder.ivAvatar.setColorFilter(Color.GRAY)
                                }
                            } else {
                                holder.ivAvatar.setImageResource(R.drawable.ic_person)
                                holder.ivAvatar.setColorFilter(Color.GRAY)
                            }
                        }
                    }
                }
                override fun getItemCount(): Int = members.size
            }
        }
    }

    private fun showKickConfirmation(username: String) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)
        tvTitle.text = "KICK MEMBER"
        tvMessage.text = "Are you sure you want to remove $username?"
        btnYes.text = "YES, REMOVE"
        btnNo.setOnClickListener { dialog.dismiss() }
        btnYes.setOnClickListener {
            kickMember(username)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun kickMember(username: String) {
        dbHelper.leaveGroup(groupId, username) { success ->
            if (success) {
                Toast.makeText(context, "$username removed", Toast.LENGTH_SHORT).show()
                dbHelper.getGroupCreator(groupId) { creator -> setupMemberList(creator) }
            } else {
                Toast.makeText(context, "Failed to remove member", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInviteDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Invite Members")
        val input = android.widget.EditText(context)
        input.hint = "Search Username"
        builder.setView(input)
        builder.setPositiveButton("Search") { _, _ ->
            val query = input.text.toString()
            if (query.isNotEmpty()) performSearch(query)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun performSearch(query: String) {
        dbHelper.searchUsers(query, currentUser) { users ->
            if (users.isEmpty()) Toast.makeText(context, "No users found", Toast.LENGTH_SHORT).show()
            else showUserListDialog(users)
        }
    }

    private fun showUserListDialog(users: ArrayList<String>) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select User to Invite")
        val userArray = users.toTypedArray()
        builder.setItems(userArray) { _, which ->
            sendInvite(userArray[which])
        }
        builder.show()
    }

    private fun sendInvite(targetUser: String) {
        dbHelper.isUserInGroup(groupId, targetUser) { exists ->
            if (exists) {
                Toast.makeText(context, "$targetUser is already in the group", Toast.LENGTH_SHORT).show()
            } else {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val displayName = if(currentGroupNickname.isNotEmpty()) currentGroupNickname else currentGroupName
                val message = "You have been invited to join $displayName"
                dbHelper.addNotification(targetUser, "Group Invitation", message, date, groupId, "GROUP_INVITE")
                Toast.makeText(context, "Invite sent to $targetUser", Toast.LENGTH_SHORT).show()
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