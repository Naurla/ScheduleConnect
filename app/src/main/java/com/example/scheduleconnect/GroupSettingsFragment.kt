package com.example.scheduleconnect

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import java.io.InputStream

class GroupSettingsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivGroupImage: ImageView
    private lateinit var tvGroupName: TextView
    private lateinit var btnEditGroupName: ImageView
    private lateinit var btnChangeImage: Button
    private lateinit var btnBack: ImageView
    private lateinit var recyclerParticipants: RecyclerView

    private var groupId: Int = -1
    private var currentGroupName: String = ""
    private var currentImageBase64: String = ""

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    val bitmap = getResizedBitmap(imageUri)
                    if (bitmap != null) {
                        saveNewGroupImage(bitmap)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_settings, container, false)
        dbHelper = DatabaseHelper(requireContext())

        groupId = arguments?.getInt("GROUP_ID") ?: -1

        ivGroupImage = view.findViewById(R.id.ivSettingsGroupImage)
        tvGroupName = view.findViewById(R.id.tvSettingsGroupName)
        btnEditGroupName = view.findViewById(R.id.btnEditGroupName)
        btnChangeImage = view.findViewById(R.id.btnChangeGroupImage)
        btnBack = view.findViewById(R.id.btnBackSettings)
        recyclerParticipants = view.findViewById(R.id.recyclerSettingsParticipants)

        recyclerParticipants.layoutManager = LinearLayoutManager(context)
        recyclerParticipants.isNestedScrollingEnabled = false

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnChangeImage.setOnClickListener { openGallery() }

        btnEditGroupName.setOnClickListener {
            showEditGroupNameDialog()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadGroupData()
        loadParticipants()
    }

    private fun loadGroupData() {
        dbHelper.getGroupDetails(groupId) { group ->
            if (group != null) {
                currentGroupName = group.name
                currentImageBase64 = group.imageUrl

                tvGroupName.text = currentGroupName

                if (currentImageBase64.isNotEmpty()) {
                    try {
                        val decodedString = Base64.decode(currentImageBase64, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        ivGroupImage.setImageBitmap(decodedByte)
                        ivGroupImage.imageTintList = null
                        ivGroupImage.setPadding(0, 0, 0, 0)
                    } catch (e: Exception) {
                        // Keep default
                    }
                }
            }
        }
    }

    private fun loadParticipants() {
        // UPDATED: Now calls getGroupMembersWithNicknames instead of just usernames
        dbHelper.getGroupMembersWithNicknames(groupId) { memberList ->
            if (memberList.isEmpty()) {
                // Optional handle empty
            }
            recyclerParticipants.adapter = ParticipantsAdapter(memberList) { username ->
                showEditMemberNicknameDialog(username)
            }
        }
    }

    // --- MODALS ---

    private fun showEditGroupNameDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Group Name")

        val input = EditText(requireContext())
        input.setText(currentGroupName)
        input.setSelection(input.text.length)

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Save") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                dbHelper.updateGroupName(groupId, newName) { success ->
                    if (success) {
                        Toast.makeText(context, "Group name updated", Toast.LENGTH_SHORT).show()
                        loadGroupData()
                    } else {
                        Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showEditMemberNicknameDialog(username: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Nickname for $username")

        val input = EditText(requireContext())
        input.hint = "Enter new nickname"

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Save") { _, _ ->
            val newNickname = input.text.toString().trim()
            // Allow empty string if they want to remove nickname, or enforce length check
            dbHelper.updateMemberNickname(groupId, username, newNickname) { success ->
                if (success) {
                    Toast.makeText(context, "Nickname updated", Toast.LENGTH_SHORT).show()
                    loadParticipants() // Refresh List immediately
                } else {
                    Toast.makeText(context, "Failed to update nickname", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveNewGroupImage(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        val byteArrays = stream.toByteArray()
        val newBase64Image = Base64.encodeToString(byteArrays, Base64.DEFAULT)

        dbHelper.updateGroupImage(groupId, newBase64Image) { success ->
            if (success) {
                Toast.makeText(context, "Image updated", Toast.LENGTH_SHORT).show()
                ivGroupImage.setImageBitmap(bitmap)
                ivGroupImage.imageTintList = null
                ivGroupImage.setPadding(0, 0, 0, 0)
            } else {
                Toast.makeText(context, "Failed to update image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val REQUIRED_SIZE = 500
            var width_tmp = options.outWidth
            var height_tmp = options.outHeight
            var scale = 1
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) break
                width_tmp /= 2
                height_tmp /= 2
                scale *= 2
            }

            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = requireContext().contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream, null, o2)
        } catch (e: Exception) {
            return null
        } finally {
            inputStream?.close()
        }
    }

    // --- UPDATED ADAPTER ---
    inner class ParticipantsAdapter(
        private val memberList: List<Map<String, String>>, // Contains username AND nickname
        private val onEditClick: (String) -> Unit
    ) : RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvUsername: TextView = view.findViewById(R.id.tvAttendeeName)
            val tvStatus: TextView = view.findViewById(R.id.tvAttendeeStatus)
            val ivAvatar: ImageView = view.findViewById(R.id.ivAttendeeAvatar)
            val btnKick: ImageView = view.findViewById(R.id.btnKickMember)
            val btnEdit: ImageView? = view.findViewById(R.id.btnEditNickname)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val memberData = memberList[position]
            val username = memberData["username"] ?: ""
            val nickname = memberData["nickname"] ?: ""

            holder.btnKick.visibility = View.GONE
            holder.ivAvatar.setImageResource(R.drawable.ic_person)
            holder.ivAvatar.setColorFilter(Color.GRAY)
            holder.ivAvatar.imageTintList = null

            holder.btnEdit?.visibility = View.VISIBLE
            holder.btnEdit?.setOnClickListener { onEditClick(username) }

            // 1. Initial set based on what we have (Nickname Priority)
            if (nickname.isNotEmpty()) {
                holder.tvUsername.text = nickname
                holder.tvStatus.text = "@$username"
                holder.tvStatus.visibility = View.VISIBLE
            } else {
                holder.tvUsername.text = username
                holder.tvStatus.visibility = View.GONE
            }

            // 2. Fetch full user details to get Image and Real Name
            dbHelper.getUserDetails(username) { user ->
                if (user != null) {
                    val realName = user.firstName.trim()

                    if (nickname.isNotEmpty()) {
                        // If Nickname exists, it stays as Main Title.
                        // Subtitle becomes Real Name if available, otherwise stays @username
                        if (realName.isNotEmpty()) {
                            holder.tvStatus.text = realName
                        }
                    } else {
                        // If No Nickname, Main Title becomes Real Name (if available)
                        // Subtitle becomes @username
                        if (realName.isNotEmpty()) {
                            holder.tvUsername.text = realName
                            holder.tvStatus.text = "@$username"
                            holder.tvStatus.visibility = View.VISIBLE
                        }
                    }

                    // Load Profile Image
                    if (user.profileImageUrl.isNotEmpty()) {
                        try {
                            val decodedString = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            if (decodedByte != null) {
                                holder.ivAvatar.setImageBitmap(decodedByte)
                                holder.ivAvatar.colorFilter = null
                            }
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        override fun getItemCount(): Int = memberList.size
    }
}
