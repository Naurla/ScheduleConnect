package com.example.scheduleconnect

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyText: TextView
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
        layoutEmpty = view.findViewById(R.id.layoutEmptyState)
        tvEmptyText = view.findViewById(R.id.tvEmpty)

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

        // Search Logic
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

    // --- FIX: Added onResume to refresh list when coming back ---
    override fun onResume() {
        super.onResume()
        loadSchedules()
    }

    private fun updateTabStyles() {
        if (isPersonal) {
            // Personal Selected: Red Background, White Text
            tabPersonal.setBackgroundResource(R.drawable.bg_tab_active)
            tabPersonal.setTextColor(Color.WHITE)

            // Shared Unselected: Transparent, Gray Text
            tabShared.setBackgroundResource(0)
            tabShared.setTextColor(Color.parseColor("#757575"))
        } else {
            // Personal Unselected
            tabPersonal.setBackgroundResource(0)
            tabPersonal.setTextColor(Color.parseColor("#757575"))

            // Shared Selected
            tabShared.setBackgroundResource(R.drawable.bg_tab_active)
            tabShared.setTextColor(Color.WHITE)
        }
    }

    private fun loadSchedules() {
        val type = if (isPersonal) "personal" else "shared"
        val sharedPref = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
        // --- FIX: Use "USERNAME" to match MainActivity ---
        val currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        dbHelper.getSchedules(currentUser, type) { fetchedList ->
            // This code runs when Firebase returns data
            allSchedules = fetchedList

            // Apply current search filter (if any text exists)
            val currentSearchText = etSearch.text.toString()
            filterList(currentSearchText)
        }
    }

    private fun filterList(query: String) {
        val filteredList = ArrayList<Schedule>()

        if (query.isEmpty()) {
            filteredList.addAll(allSchedules)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            for (item in allSchedules) {
                // Check Title or Location
                if (item.title.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    item.location.lowercase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(item)
                }
            }
        }

        // Update Adapter
        adapter.updateList(filteredList)

        // Show/Hide Empty State
        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
            tvEmptyText.text = if (query.isEmpty()) "No schedules found" else "No results found"
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
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
        // --- FIX: Added Image URL passing ---
        bundle.putString("SCH_IMAGE", schedule.imageUrl)

        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}