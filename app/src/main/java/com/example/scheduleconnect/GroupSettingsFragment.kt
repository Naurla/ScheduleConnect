package com.example.scheduleconnect

import android.app.Activity
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
    private lateinit var etNickname: EditText
    private lateinit var btnSave: Button
    private lateinit var btnChangeImage: Button
    private lateinit var btnBack: ImageView
    private lateinit var recyclerParticipants: RecyclerView

    private var groupId: Int = -1
    private var currentImageBase64: String = ""
    private var selectedImageBitmap: Bitmap? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    selectedImageBitmap = getResizedBitmap(imageUri)
                    ivGroupImage.setImageBitmap(selectedImageBitmap)
                    ivGroupImage.imageTintList = null
                    ivGroupImage.setPadding(0, 0, 0, 0)
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
        currentImageBase64 = arguments?.getString("CURRENT_IMAGE") ?: ""
        val currentNickname = arguments?.getString("CURRENT_NICKNAME") ?: ""

        ivGroupImage = view.findViewById(R.id.ivSettingsGroupImage)
        etNickname = view.findViewById(R.id.etGroupNickname)
        btnSave = view.findViewById(R.id.btnSaveGroupSettings)
        btnChangeImage = view.findViewById(R.id.btnChangeGroupImage)
        btnBack = view.findViewById(R.id.btnBackSettings)
        recyclerParticipants = view.findViewById(R.id.recyclerSettingsParticipants)

        etNickname.setText(currentNickname)
        recyclerParticipants.layoutManager = LinearLayoutManager(context)
        recyclerParticipants.isNestedScrollingEnabled = false

        if (currentImageBase64.isNotEmpty()) {
            try {
                val decodedString = Base64.decode(currentImageBase64, Base64.DEFAULT)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivGroupImage.setImageBitmap(decodedByte)
                ivGroupImage.imageTintList = null
                ivGroupImage.setPadding(0, 0, 0, 0)
            } catch (e: Exception) { }
        }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnChangeImage.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveSettings() }

        loadParticipants()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Ensure the participant list is refreshed every time this fragment becomes visible
        loadParticipants()
    }

    private fun loadParticipants() {
        dbHelper.getGroupMemberUsernames(groupId, "") { usernames ->
            if (usernames.isEmpty()) {
                Toast.makeText(context, "No members found", Toast.LENGTH_SHORT).show()
            }
            recyclerParticipants.adapter = ParticipantsAdapter(usernames) { username ->
                showEditNicknameDialog(username)
            }
        }
    }

    inner class ParticipantsAdapter(
        private val usernames: List<String>,
        private val onEditClick: (String) -> Unit
    ) : RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvUsername: TextView = view.findViewById(R.id.tvAttendeeName)
            val tvStatus: TextView = view.findViewById(R.id.tvAttendeeStatus)
            val ivAvatar: ImageView = view.findViewById(R.id.ivAttendeeAvatar)
            val btnKick: ImageView = view.findViewById(R.id.btnKickMember)
            // CONFIRMED ID from XML: btnEditNickname
            val btnEdit: ImageView? = view.findViewById(R.id.btnEditNickname)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val username = usernames[position]

            holder.tvUsername.text = username
            holder.tvStatus.visibility = View.GONE
            holder.btnKick.visibility = View.GONE

            // Set Placeholder image and color filter (Ensuring gray placeholder)
            holder.ivAvatar.setImageResource(R.drawable.ic_person)
            holder.ivAvatar.setColorFilter(Color.GRAY)
            holder.ivAvatar.imageTintList = null

            // Set Edit Click Listener (Pencil Icon)
            holder.btnEdit?.setOnClickListener {
                onEditClick(username)
            }

            dbHelper.getUserDetails(username) { user ->
                if (user != null) {
                    val realName = user.firstName.trim()

                    // Display real name and username (@...)
                    if (realName.isNotEmpty()) {
                        holder.tvUsername.text = realName
                        holder.tvStatus.text = "@$username"
                        holder.tvStatus.visibility = View.VISIBLE
                    } else {
                        // Fallback to username if real name is empty (like 'Adi')
                        holder.tvUsername.text = username
                        holder.tvStatus.visibility = View.GONE
                    }

                    // Load Image Logic (FIXED for robustness and image display)
                    if (user.profileImageUrl.isNotEmpty()) {
                        try {
                            val decodedString = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                            if (decodedByte != null) {
                                holder.ivAvatar.setImageBitmap(decodedByte)
                                holder.ivAvatar.colorFilter = null // Remove filter when image loads
                            }
                        } catch (e: Exception) {
                            // Keep placeholder if decode fails
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int = usernames.size
    }

    private fun showEditNicknameDialog(username: String) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Nickname for $username")

        val input = EditText(requireContext())
        input.hint = "Enter new nickname"
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val newNickname = input.text.toString().trim()
            if (newNickname.isNotEmpty()) {
                // NOTE: You need to add this function to your DatabaseHelper!
                // dbHelper.updateMemberNickname(groupId, username, newNickname)

                Toast.makeText(context, "Nickname updated (Logic needed in DB)", Toast.LENGTH_SHORT).show()

                // Reload list to show changes
                loadParticipants()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveSettings() {
        val nickname = etNickname.text.toString().trim()
        var newBase64Image = ""

        if (selectedImageBitmap != null) {
            val stream = ByteArrayOutputStream()
            selectedImageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val byteArrays = stream.toByteArray()
            newBase64Image = Base64.encodeToString(byteArrays, Base64.DEFAULT)
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        dbHelper.updateGroupDetails(groupId, nickname, newBase64Image) { success ->
            btnSave.isEnabled = true
            btnSave.text = "SAVE CHANGES"
            if (success) {
                Toast.makeText(context, "Group Info Updated!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Error updating info", Toast.LENGTH_SHORT).show()
            }
        }
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
}