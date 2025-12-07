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
    private lateinit var adapter: HistoryAdapter

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
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        val allSchedules = dbHelper.getAllHistorySchedules(currentUser)
        val historyList = ArrayList<Schedule>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = Date()

        for (item in allSchedules) {
            // 1. Check Explicit Status
            if (item.status == "FINISHED" || item.status == "CANCELLED") {
                historyList.add(item)
                continue
            }
            // 2. Check Date (Past Active)
            try {
                val dateObj = sdf.parse(item.date)
                if (dateObj != null && dateObj.before(now)) {
                    historyList.add(item)
                }
            } catch (e: Exception) {
                continue
            }
        }

        if (historyList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            adapter = HistoryAdapter(historyList)
            adapter.setOnItemClickListener { schedule ->
                openHistoryDetails(schedule)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun openHistoryDetails(schedule: Schedule) {
        val fragment = ScheduleDetailFragment()
        val bundle = Bundle()
        bundle.putInt("SCH_ID", schedule.id)
        bundle.putString("SCH_TITLE", schedule.title)
        bundle.putString("SCH_DATE", schedule.date)
        bundle.putString("SCH_LOC", schedule.location)
        bundle.putString("SCH_DESC", schedule.description)
        bundle.putString("SCH_CREATOR", schedule.creator)
        bundle.putString("SCH_TYPE", schedule.type)

        // --- NEW: Pass flag to indicate this is strictly history view ---
        bundle.putBoolean("IS_FROM_HISTORY", true)

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}