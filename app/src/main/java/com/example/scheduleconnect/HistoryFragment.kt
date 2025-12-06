package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerHistory)
        tvEmpty = view.findViewById(R.id.tvEmptyHistory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadHistory()

        return view
    }

    private fun loadHistory() {
        // 1. Safe User Retrieval (Fixes the Null Error)
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        // We use "default_user" as a fallback so 'currentUser' is never null
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // 2. Fetch ALL schedules (Personal + Shared)
        val personalList = dbHelper.getSchedules(currentUser, "personal")
        val sharedList = dbHelper.getSchedules(currentUser, "shared")

        val allSchedules = ArrayList<Schedule>()
        allSchedules.addAll(personalList)
        allSchedules.addAll(sharedList)

        // 3. Filter for Past Dates
        val historyList = ArrayList<Schedule>()
        // Ensure this format matches exactly how you save it in AddScheduleFragment
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = Date()

        for (item in allSchedules) {
            try {
                // Try to parse the date string
                val dateObj = sdf.parse(item.date)
                // If date is valid and is BEFORE now, add to history
                if (dateObj != null && dateObj.before(now)) {
                    historyList.add(item)
                }
            } catch (e: Exception) {
                // If date parsing fails (e.g. data saved before the format update), ignore it
                continue
            }
        }

        // 4. Update UI
        if (historyList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            // Reuse the existing ScheduleAdapter
            recyclerView.adapter = ScheduleAdapter(historyList)
        }
    }
}