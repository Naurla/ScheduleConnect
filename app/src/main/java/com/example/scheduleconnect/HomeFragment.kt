package com.example.scheduleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabPersonal: TextView
    private lateinit var tabShared: TextView

    // Flag to track which tab is active
    private var isPersonal = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerSchedules)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tabPersonal = view.findViewById(R.id.tabYourSchedule)
        tabShared = view.findViewById(R.id.tabSharedSchedule)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Tab Logic
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

        // Initialize styles and load data
        updateTabStyles()
        loadSchedules()

        return view
    }

    private fun updateTabStyles() {
        if (isPersonal) {
            // Case: "YOUR SCHEDULES" is Active
            tabPersonal.setTextColor(resources.getColor(R.color.app_red, null))
            tabShared.setTextColor(resources.getColor(R.color.black, null))
        } else {
            // Case: "SHARED SCHEDULES" is Active
            tabPersonal.setTextColor(resources.getColor(R.color.black, null))
            tabShared.setTextColor(resources.getColor(R.color.app_red, null))
        }
    }

    private fun loadSchedules() {
        val type = if (isPersonal) "personal" else "shared"

        val sharedPref = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        val list = dbHelper.getSchedules(currentUser, type)

        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val adapter = ScheduleAdapter(list)
            recyclerView.adapter = adapter

            adapter.setOnItemClickListener { schedule ->
                val fragment = ScheduleDetailFragment()
                val bundle = Bundle()
                bundle.putInt("SCH_ID", schedule.id)
                bundle.putString("SCH_TITLE", schedule.title)
                bundle.putString("SCH_DATE", schedule.date)
                bundle.putString("SCH_LOC", schedule.location)
                bundle.putString("SCH_DESC", schedule.description)
                bundle.putString("SCH_CREATOR", schedule.creator)

                // --- NEW: PASS THE TYPE ---
                bundle.putString("SCH_TYPE", schedule.type)

                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}