package com.example.scheduleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerHistory)
        layoutEmpty = view.findViewById(R.id.layoutEmptyHistory)
        tvEmpty = view.findViewById(R.id.tvEmptyHistory)

        recyclerView.layoutManager = LinearLayoutManager(context)

        loadHistory()

        return view
    }

    private fun loadHistory() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // This call will now work because 'getHistorySchedules' is in the new DatabaseHelper
        dbHelper.getHistorySchedules(currentUser) { historyList ->

            if (historyList.isEmpty()) {
                recyclerView.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE

                // Sort by ID (newest first)
                historyList.sortByDescending { it.id }

                adapter = HistoryAdapter(historyList)
                recyclerView.adapter = adapter
            }
        }
    }
}