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
import android.util.Base64
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

    private lateinit var ivGroupPreview: ImageView
    private var selectedBitmap: Bitmap? = null

    // Image Picker Launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    selectedBitmap = getResizedBitmap(imageUri)
                    ivGroupPreview.setImageBitmap(selectedBitmap)
                    // Remove padding and tint when actual image is set
                    ivGroupPreview.setPadding(0, 0, 0, 0)
                    ivGroupPreview.imageTintList = null
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

        // Image Selection
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

        recycler = view.findViewById(R.id.recyclerUserResults)
        cardResults = view.findViewById(R.id.cardSearchResults)

        // Setup Recycler
        recycler.layoutManager = LinearLayoutManager(context)
        adapter = UserInviteAdapter(ArrayList()) { username ->
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
                    // --- ASYNC SEARCH ---
                    dbHelper.searchUsers(query, currentUser) { results ->
                        if (results.isNotEmpty()) {
                            adapter.updateList(results)
                            cardResults.visibility = View.VISIBLE
                        } else {
                            cardResults.visibility = View.GONE
                        }
                    }
                } else {
                    adapter.updateList(ArrayList())
                    cardResults.visibility = View.GONE
                }
            }
        })

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // --- CREATE GROUP LOGIC (UPDATED WITH BASE64) ---
        btnCreate.setOnClickListener {
            val groupName = etGroupName.text.toString().trim()

            if (groupName.isEmpty()) {
                etGroupName.error = "Group Name required"
                return@setOnClickListener
            }

            val code = UUID.randomUUID().toString().substring(0, 6).uppercase()

            // 1. Convert Image to Base64 String
            var base64Image = ""
            if (selectedBitmap != null) {
                val stream = ByteArrayOutputStream()
                // Compress heavily (50) to keep string short for Firestore
                selectedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                val byteArrays = stream.toByteArray()
                base64Image = Base64.encodeToString(byteArrays, Base64.DEFAULT)
            }

            // 2. Disable button to prevent double clicks
            btnCreate.isEnabled = false
            btnCreate.text = "Creating..."

            // 3. Call Database Helper using the NEW method
            dbHelper.createGroupWithBase64(groupName, code, currentUser, base64Image) { newGroupId ->

                // Ensure UI updates run on main thread
                activity?.runOnUiThread {
                    btnCreate.isEnabled = true
                    btnCreate.text = "Create Group"

                    if (newGroupId != -1) {
                        // Success: Add invited users
                        for (user in selectedUsers) {
                            dbHelper.addMemberToGroup(newGroupId, user) {
                                // Optional: Log success
                            }
                        }
                        Toast.makeText(context, "Group Created! Code: $code", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        // Failure
                        Toast.makeText(context, "Failed to create group. Check connection.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return view
    }

    private fun getResizedBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val REQUIRED_SIZE = 300 // Reduced size for Firestore
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