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
    private lateinit var recyclerMembers: RecyclerView

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

        btnAddSchedule = view.findViewById(R.id.btnAddGroupSchedule)
        btnLeave = view.findViewById(R.id.btnLeaveGroup)
        btnDelete = view.findViewById(R.id.btnDeleteGroup)
        btnBack = view.findViewById(R.id.btnBackGroupDetail)
        btnChat = view.findViewById(R.id.btnGroupChat)

        tvName.text = groupName
        tvCode.text = groupCode

        // --- NEW: FETCH IMAGE FROM DATABASE ---
        loadGroupDetails()

        // --- ASYNC: Get Creator info to set button visibility ---
        dbHelper.getGroupCreator(groupId) { creator ->
            tvCreator.text = "Created by: $creator"

            // Logic for Admin/Member buttons
            if (currentUser == creator) {
                btnDelete.visibility = View.VISIBLE
                btnLeave.visibility = View.GONE
            } else {
                btnDelete.visibility = View.GONE
                btnLeave.visibility = View.VISIBLE
            }

            // Load members after we know who the creator is (for styling)
            setupMemberList(creator)
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // --- CHAT BUTTON LOGIC ---
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

        // Add Shared Schedule Logic
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

    private fun loadGroupDetails() {
        dbHelper.getGroupDetails(groupId) { group ->
            if (group != null) {
                tvName.text = group.name

                // --- IMAGE DECODING LOGIC ---
                val imageUrl = group.imageUrl
                if (imageUrl.isNotEmpty()) {
                    if (imageUrl.startsWith("http")) {
                        // Legacy: Use Glide if it's a URL
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_group)
                            .centerCrop()
                            .into(ivGroupImage)
                    } else {
                        // New: Decode Base64 string
                        try {
                            val decodedString = Base64.decode(imageUrl, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            ivGroupImage.setImageBitmap(decodedByte)
                        } catch (e: Exception) {
                            ivGroupImage.setImageResource(R.drawable.ic_group)
                        }
                    }
                    // Remove tint and padding for real photo
                    ivGroupImage.setPadding(0, 0, 0, 0)
                    ivGroupImage.imageTintList = null
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

                        dbHelper.getProfilePictureUrl(memberName) { url ->
                            if (url.isNotEmpty()) {
                                Glide.with(holder.itemView.context)
                                    .load(url)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_person)
                                    .into(holder.ivAvatar)

                                holder.ivAvatar.colorFilter = null
                            } else {
                                holder.ivAvatar.setImageResource(R.drawable.ic_person)
                                holder.ivAvatar.setColorFilter(Color.parseColor("#999999"))
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

        btnYes.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun leaveGroup() {
        dbHelper.leaveGroup(groupId, currentUser) { success ->
            if (success) {
                Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Failed to leave group", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteGroup() {
        dbHelper.deleteGroup(groupId) { success ->
            if (success) {
                Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Failed to delete group", Toast.LENGTH_SHORT).show()
            }
        }
    }
}