package com.example.scheduleconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

class CreateGroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: UserInviteAdapter
    private lateinit var cardResults: CardView
    private val selectedUsers = ArrayList<String>()

    // --- NEW: Image Variables ---
    private lateinit var ivGroupPreview: ImageView
    private var selectedBitmap: Bitmap? = null

    // --- NEW: Image Picker Launcher ---
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    // Resize to prevent crash
                    selectedBitmap = getResizedBitmap(imageUri)
                    ivGroupPreview.setImageBitmap(selectedBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // UI References
        val etGroupName = view.findViewById<EditText>(R.id.etGroupName)
        val etSearch = view.findViewById<EditText>(R.id.etAddPeople)
        val btnCreate = view.findViewById<Button>(R.id.btnFinalizeCreate)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackCreate)

        // --- NEW: Bind Image Views and Set Listener ---
        val btnSelectImg = view.findViewById<Button>(R.id.btnSelectGroupImage)
        ivGroupPreview = view.findViewById(R.id.ivGroupImagePreview)

        btnSelectImg.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                imagePickerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Cannot open gallery", Toast.LENGTH_SHORT).show()
            }
        }
        // -----------------------------

        recycler = view.findViewById(R.id.recyclerUserResults)
        cardResults = view.findViewById(R.id.cardSearchResults)

        // Setup Recycler
        recycler.layoutManager = LinearLayoutManager(context)
        adapter = UserInviteAdapter(ArrayList()) { username ->
            // Callback when "INVITE +" is clicked
            if (!selectedUsers.contains(username)) {
                selectedUsers.add(username)
                Toast.makeText(context, "$username added to invite list", Toast.LENGTH_SHORT).show()
            }
        }
        recycler.adapter = adapter

        // Get Current User
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // Search Logic
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    val results = dbHelper.searchUsers(query, currentUser)

                    if (results.isNotEmpty()) {
                        // Show the grey box only if we have results
                        adapter.updateList(results)
                        cardResults.visibility = View.VISIBLE
                    } else {
                        // Hide if no matches found
                        cardResults.visibility = View.GONE
                    }
                } else {
                    // Hide if search bar is empty
                    adapter.updateList(ArrayList())
                    cardResults.visibility = View.GONE
                }
            }
        })

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnCreate.setOnClickListener {
            val groupName = etGroupName.text.toString()

            if (groupName.isEmpty()) {
                etGroupName.error = "Group Name required"
                return@setOnClickListener
            }

            // Generate Code
            val code = UUID.randomUUID().toString().substring(0, 6).uppercase()

            // --- NEW: Convert Bitmap to ByteArray ---
            var imageBytes: ByteArray? = null
            if (selectedBitmap != null) {
                val stream = ByteArrayOutputStream()
                selectedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                imageBytes = stream.toByteArray()
            }

            // 1. Create Group (Updated Method Call)
            val newGroupId = dbHelper.createGroupGetId(groupName, code, currentUser, imageBytes)

            if (newGroupId != -1) {
                // 2. Add Invited Users
                for (user in selectedUsers) {
                    dbHelper.addMemberToGroup(newGroupId, user)
                }

                Toast.makeText(context, "Group Created with ${selectedUsers.size} members! Code: $code", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, "Failed to create group", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // --- NEW: Helper to Resize Image ---
    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            // 1. Decode dimensions only
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // 2. Calculate scale factor (Target 500px)
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

            // 3. Load actual image with scaling
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = requireContext().contentResolver.openInputStream(uri)
            val scaledBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            return scaledBitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }
}