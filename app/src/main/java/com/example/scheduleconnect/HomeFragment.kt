package com.example.scheduleconnect

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabPersonal: TextView
    private lateinit var tabShared: TextView
    private lateinit var etSearch: EditText

    // Adapter reference to update it later
    private lateinit var adapter: ScheduleAdapter

    // Keep a copy of the original full list to filter from
    private var allSchedules = ArrayList<Schedule>()
    private var isPersonal = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerSchedules)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tabPersonal = view.findViewById(R.id.tabYourSchedule)
        tabShared = view.findViewById(R.id.tabSharedSchedule)
        etSearch = view.findViewById(R.id.etSearch)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter with empty list first
        adapter = ScheduleAdapter(ArrayList())
        recyclerView.adapter = adapter

        // Handle Item Clicks
        adapter.setOnItemClickListener { schedule ->
            openScheduleDetails(schedule)
        }

        // Tab Listeners
        tabPersonal.setOnClickListener {
            isPersonal = true
            updateTabStyles()
            loadSchedules()
        }

        tabShared.setOnClickListener {
            isPersonal = false
            updateTabStyles()
            loadSchedules()
        }

        // --- NEW: Search Logic ---
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        updateTabStyles()
        loadSchedules()

        return view
    }

    // --- FIX: Updated to dynamically apply the red underline/selector ---
    private fun updateTabStyles() {
        if (isPersonal) {
            // Personal is selected: Apply red underline and color
            tabPersonal.setTextColor(Color.parseColor("#8B1A1A"))
            tabPersonal.setBackgroundResource(R.drawable.tab_selector)

            // Shared is unselected: Remove underline and use black color
            tabShared.setTextColor(Color.parseColor("#000000"))
            tabShared.setBackgroundResource(0)
        } else {
            // Personal is unselected
            tabPersonal.setTextColor(Color.parseColor("#000000"))
            tabPersonal.setBackgroundResource(0)

            // Shared is selected
            tabShared.setTextColor(Color.parseColor("#8B1A1A"))
            tabShared.setBackgroundResource(R.drawable.tab_selector)
        }
    }

    private fun loadSchedules() {
        val type = if (isPersonal) "personal" else "shared"
        val sharedPref = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // 1. Fetch from DB
        allSchedules = dbHelper.getSchedules(currentUser, type)

        // 2. Apply current search filter (if any text exists)
        val currentSearchText = etSearch.text.toString()
        filterList(currentSearchText)
    }

    // --- NEW: Filter Function ---
    private fun filterList(query: String) {
        val filteredList = ArrayList<Schedule>()

        if (query.isEmpty()) {
            filteredList.addAll(allSchedules)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            for (item in allSchedules) {
                // Check Title or Description or Location
                if (item.title.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    item.location.lowercase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(item)
                }
            }
        }

        // Update Adapter
        adapter.updateList(filteredList)

        // Show/Hide Empty Text
        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (query.isEmpty()) "No schedules found" else "No result found"
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun openScheduleDetails(schedule: Schedule) {
        val fragment = ScheduleDetailFragment()
        val bundle = Bundle()
        bundle.putInt("SCH_ID", schedule.id)
        bundle.putString("SCH_TITLE", schedule.title)
        bundle.putString("SCH_DATE", schedule.date)
        bundle.putString("SCH_LOC", schedule.location)
        bundle.putString("SCH_DESC", schedule.description)
        bundle.putString("SCH_CREATOR", schedule.creator)
        bundle.putString("SCH_TYPE", schedule.type)

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}