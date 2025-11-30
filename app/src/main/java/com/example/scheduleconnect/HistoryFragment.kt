package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var adapter: HistoryAdapter
    private var fullList = ArrayList<Schedule>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerHistory = view.findViewById(R.id.recyclerHistory)
        etSearch = view.findViewById(R.id.etSearchHistory)

        recyclerHistory.layoutManager = LinearLayoutManager(context)

        loadHistory()

        // Search Logic
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
        })

        return view
    }

    private fun loadHistory() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // Pass null as type to get ALL schedules (Personal + Shared)
        fullList = dbHelper.getSchedules(currentUser, null)

        adapter = HistoryAdapter(fullList)
        recyclerHistory.adapter = adapter
    }

    private fun filter(text: String) {
        val filteredList = ArrayList<Schedule>()
        for (item in fullList) {
            if (item.title.lowercase().contains(text.lowercase())) {
                filteredList.add(item)
            }
        }
        adapter.updateList(filteredList)
    }
}