package com.example.scheduleconnect


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream
import java.util.UUID


class CreateGroupFragment : Fragment() {


    private lateinit var etGroupName: EditText
    private lateinit var ivGroupImage: ImageView
    private lateinit var btnUploadImage: Button
    private lateinit var btnAddMembers: Button
    private lateinit var rvSelectedMembers: RecyclerView
    private lateinit var btnCreate: Button
    private lateinit var btnBack: ImageView // Added this
    private lateinit var dbHelper: DatabaseHelper


    private var imageBytes: ByteArray? = null
    private val selectedMembers = ArrayList<String>()
    private lateinit var selectedMembersAdapter: SelectedMembersAdapter


    // Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data!!.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                ivGroupImage.setImageBitmap(bitmap)
                ivGroupImage.setBackgroundColor(Color.TRANSPARENT)
                ivGroupImage.setPadding(0, 0, 0, 0)
                ivGroupImage.imageTintList = null


                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                imageBytes = stream.toByteArray()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_group, container, false)
        dbHelper = DatabaseHelper(requireContext())


        etGroupName = view.findViewById(R.id.etGroupName)
        ivGroupImage = view.findViewById(R.id.ivGroupImage)
        btnUploadImage = view.findViewById(R.id.btnUploadGroupImage)
        btnAddMembers = view.findViewById(R.id.btnAddGroupMembers)
        rvSelectedMembers = view.findViewById(R.id.recyclerSelectedMembers)
        btnCreate = view.findViewById(R.id.btnCreateGroupAction)
        btnBack = view.findViewById(R.id.btnBackCreate) // Initialize it


        // Setup Selected Members List (Horizontal)
        rvSelectedMembers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        selectedMembersAdapter = SelectedMembersAdapter(selectedMembers) { userToRemove ->
            selectedMembers.remove(userToRemove)
            selectedMembersAdapter.notifyDataSetChanged()
        }
        rvSelectedMembers.adapter = selectedMembersAdapter


        btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }


        btnAddMembers.setOnClickListener {
            showAddMemberDialog()
        }


        btnCreate.setOnClickListener {
            createGroup()
        }


        // Fix for Back Button
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        return view
    }


    private fun createGroup() {
        val groupName = etGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
            return
        }


        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val creator = sharedPref.getString("USERNAME", "default_user") ?: "default_user"


        val code = UUID.randomUUID().toString().substring(0, 6).uppercase()


        dbHelper.createGroup(groupName, code, creator, imageBytes) { groupId ->
            if (groupId != -1) {
                if (selectedMembers.isNotEmpty()) {
                    addMembersToGroup(groupId, 0)
                } else {
                    finishCreation()
                }
            } else {
                Toast.makeText(context, "Failed to create group", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun addMembersToGroup(groupId: Int, index: Int) {
        if (index >= selectedMembers.size) {
            finishCreation()
            return
        }
        val member = selectedMembers[index]
        dbHelper.addMemberToGroup(groupId, member) {
            addMembersToGroup(groupId, index + 1)
        }
    }


    private fun finishCreation() {
        Toast.makeText(context, "Group Created Successfully!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }


    // ==========================================
    //  SEARCH DIALOG
    // ==========================================
    private fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_invite_member, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val etSearch = dialogView.findViewById<EditText>(R.id.etSearchUser)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseInvite)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerInviteResults)
        val tvNoResults = dialogView.findViewById<TextView>(R.id.tvNoResults)


        // If you are using the old XML for dialog, hide the button.
        // If you are using the NEW XML (Step 8), this line is safe to remove or keep in try-catch/null check.




        recyclerView.layoutManager = LinearLayoutManager(context)


        val searchAdapter = UserSearchAdapter(ArrayList()) { userToAdd ->
            if (!selectedMembers.contains(userToAdd)) {
                selectedMembers.add(userToAdd)
                selectedMembersAdapter.notifyDataSetChanged()
                Toast.makeText(context, "Added $userToAdd", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        recyclerView.adapter = searchAdapter


        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("USERNAME", "") ?: ""


        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}


            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    dbHelper.searchUsers(query, currentUser) { users ->
                        if (users.isEmpty()) {
                            tvNoResults.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            tvNoResults.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            searchAdapter.updateList(users)
                        }
                    }
                } else {
                    searchAdapter.updateList(ArrayList())
                    recyclerView.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })


        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }


    // ==========================================
    //  ADAPTER 1: SEARCH RESULTS
    // ==========================================
    inner class UserSearchAdapter(
        private var userList: ArrayList<String>,
        private val onUserClick: (String) -> Unit
    ) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvInviteName)
            val tvUsername: TextView = view.findViewById(R.id.tvInviteUsername)
            val ivAvatar: ImageView = view.findViewById(R.id.ivInviteAvatar)
            val btnAction: Button = view.findViewById(R.id.btnSendInvite)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_invite_user, parent, false)
            return ViewHolder(v)
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val username = userList[position]
            holder.tvUsername.text = "@$username"
            holder.tvName.text = username


            holder.ivAvatar.setImageResource(R.drawable.ic_person)
            holder.ivAvatar.setColorFilter(Color.GRAY)
            holder.ivAvatar.imageTintList = null


            if (selectedMembers.contains(username)) {
                holder.btnAction.text = "ADDED"
                holder.btnAction.isEnabled = false
                holder.btnAction.setBackgroundColor(Color.GRAY)
            } else {
                holder.btnAction.text = "ADD"
                holder.btnAction.isEnabled = true
                holder.btnAction.setBackgroundColor(Color.parseColor("#8B1A1A"))
            }


            holder.btnAction.setOnClickListener {
                onUserClick(username)
                notifyItemChanged(position)
            }


            dbHelper.getUserDetails(username) { user ->
                if (user != null) {
                    if (user.firstName.isNotEmpty()) holder.tvName.text = user.firstName


                    if (user.profileImageUrl.isNotEmpty()) {
                        try {
                            val decodedString = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            if (decodedByte != null) {
                                holder.ivAvatar.setImageBitmap(decodedByte)
                                holder.ivAvatar.colorFilter = null
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }


        override fun getItemCount(): Int = userList.size


        fun updateList(newList: ArrayList<String>) {
            userList = newList
            notifyDataSetChanged()
        }
    }


    // ==========================================
    //  ADAPTER 2: SELECTED MEMBERS
    // ==========================================
    inner class SelectedMembersAdapter(
        private val members: ArrayList<String>,
        private val onRemoveClick: (String) -> Unit
    ) : RecyclerView.Adapter<SelectedMembersAdapter.ViewHolder>() {


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvUsername: TextView = view.findViewById(R.id.tvAttendeeName)
            val btnRemove: ImageView = view.findViewById(R.id.btnKickMember)
            val ivAvatar: ImageView = view.findViewById(R.id.ivAttendeeAvatar)
            val tvStatus: TextView = view.findViewById(R.id.tvAttendeeStatus)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
            return ViewHolder(v)
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val username = members[position]
            holder.tvUsername.text = username
            holder.tvStatus.visibility = View.GONE
            holder.btnRemove.visibility = View.VISIBLE
            holder.btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            holder.btnRemove.setOnClickListener { onRemoveClick(username) }


            holder.ivAvatar.setImageResource(R.drawable.ic_person)
            holder.ivAvatar.setColorFilter(Color.GRAY)
            holder.ivAvatar.imageTintList = null


            dbHelper.getProfilePictureUrl(username) { url ->
                if (url.isNotEmpty()) {
                    try {
                        val decodedString = Base64.decode(url, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        if (decodedByte != null) {
                            holder.ivAvatar.setImageBitmap(decodedByte)
                            holder.ivAvatar.colorFilter = null
                        }
                    } catch (e: Exception) {}
                }
            }
        }


        override fun getItemCount(): Int = members.size
    }
}

