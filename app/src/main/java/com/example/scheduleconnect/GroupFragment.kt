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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class GroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: GroupAdapter
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText // NEW: Search Bar

    // Keep the full list so we can filter from it
    private var allGroups = ArrayList<GroupInfo>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val btnCreate = view.findViewById<Button>(R.id.btnCreateGroup)
        val btnJoin = view.findViewById<Button>(R.id.btnJoinGroup)

        recycler = view.findViewById(R.id.recyclerGroups)
        layoutEmpty = view.findViewById(R.id.layoutEmptyGroups)
        tvEmpty = view.findViewById(R.id.tvEmptyGroups)
        etSearch = view.findViewById(R.id.etSearchGroups) // NEW

        recycler.layoutManager = LinearLayoutManager(context)

        // Initialize adapter with empty list
        adapter = GroupAdapter(ArrayList()) { group ->
            // Open Details on Click
            val fragment = GroupDetailsFragment()
            val bundle = Bundle()
            bundle.putInt("GROUP_ID", group.id)
            bundle.putString("GROUP_NAME", group.name)
            bundle.putString("GROUP_CODE", group.code)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        recycler.adapter = adapter

        btnCreate.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CreateGroupFragment())
                .addToBackStack(null)
                .commit()
        }

        btnJoin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, JoinGroupFragment())
                .addToBackStack(null)
                .commit()
        }

        // --- NEW: Search Logic ---
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        loadUserGroups()

        return view
    }

    private fun loadUserGroups() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        dbHelper.getUserGroups(currentUser) { groups ->
            allGroups = groups // Save the full list

            // Apply any existing search text (e.g., if screen rotated)
            filterList(etSearch.text.toString())
        }
    }

    private fun filterList(query: String) {
        val filteredList = ArrayList<GroupInfo>()

        if (query.isEmpty()) {
            filteredList.addAll(allGroups)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            for (group in allGroups) {
                // Search by Name OR Code
                if (group.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    group.code.lowercase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(group)
                }
            }
        }

        // Update Adapter
        if (filteredList.isEmpty()) {
            recycler.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (query.isEmpty()) "No groups joined" else "No groups found"
        } else {
            recycler.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            adapter.updateList(filteredList)
        }
    }
}