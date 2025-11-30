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

        loadSchedules() // Load default
        return view
    }

    private fun updateTabStyles() {
        if (isPersonal) {
            tabPersonal.setTextColor(resources.getColor(R.color.black, null))
            tabShared.setTextColor(resources.getColor(R.color.app_red, null))
        } else {
            tabPersonal.setTextColor(resources.getColor(R.color.app_red, null))
            tabShared.setTextColor(resources.getColor(R.color.black, null))
        }
    }

    private fun loadSchedules() {
        val type = if (isPersonal) "personal" else "shared"
        val list = dbHelper.getSchedules(type)

        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            recyclerView.adapter = ScheduleAdapter(list)
        }
    }
}