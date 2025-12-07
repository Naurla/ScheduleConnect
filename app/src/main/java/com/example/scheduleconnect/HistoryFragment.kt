package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerHistory)
        layoutEmpty = view.findViewById(R.id.layoutEmptyHistory)

        recyclerView.layoutManager = LinearLayoutManager(context)

        loadHistory()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // Fetch schedules (Assuming dbHelper.getAllHistorySchedules filters for Finished/Cancelled)
        // If your DB method returns all, we might need to filter here.
        // Based on previous context, getAllHistorySchedules logic usually handles this.
        val fullList = dbHelper.getAllHistorySchedules(currentUser)

        // Filter specifically for history items just in case the query returns active ones too
        val historyList = ArrayList(fullList.filter {
            it.status == "FINISHED" || it.status == "CANCELLED"
        })

        if (historyList.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            adapter = HistoryAdapter(historyList)
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
                bundle.putString("SCH_TYPE", schedule.type)
                bundle.putBoolean("IS_FROM_HISTORY", true) // Flag to hide edit buttons

                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}