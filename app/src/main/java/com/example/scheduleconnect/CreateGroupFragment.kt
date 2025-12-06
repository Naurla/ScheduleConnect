package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class CreateGroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: UserInviteAdapter
    private lateinit var cardResults: CardView // Reference to the grey box
    private val selectedUsers = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // UI References
        val etGroupName = view.findViewById<EditText>(R.id.etGroupName)
        val etSearch = view.findViewById<EditText>(R.id.etAddPeople)
        val btnCreate = view.findViewById<Button>(R.id.btnFinalizeCreate)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackCreate)

        recycler = view.findViewById(R.id.recyclerUserResults)
        cardResults = view.findViewById(R.id.cardSearchResults) // Initialize the card view

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

            // 1. Create Group
            val newGroupId = dbHelper.createGroupGetId(groupName, code, currentUser)

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
}