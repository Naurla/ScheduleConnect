package com.example.scheduleconnect

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class UserNotificationsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: NotificationAdapter
    private var allNotifications = ArrayList<Map<String, String>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user_notifications, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerNotifications)
        tvEmpty = view.findViewById(R.id.tvNoNotifications)
        val etSearch = view.findViewById<EditText>(R.id.etSearchNotif)
        val btnFilterDate = view.findViewById<TextView>(R.id.btnFilterDate)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackNotifList)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Load Data
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        allNotifications = dbHelper.getUserNotifications(currentUser)
        adapter = NotificationAdapter(allNotifications)
        recyclerView.adapter = adapter

        updateEmptyState(allNotifications)

        // Back Button
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Search Logic
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString(), null)
            }
        })

        // Filter by Date Logic
        btnFilterDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                // Ensure this format matches what is stored in DB
                val selectedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                filterList(etSearch.text.toString(), selectedDate)
                btnFilterDate.text = "Filter By: $selectedDate"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        return view
    }

    private fun filterList(query: String, dateFilter: String?) {
        val filtered = ArrayList<Map<String, String>>()
        for (item in allNotifications) {
            val title = item["title"]?.lowercase() ?: ""
            val message = item["message"]?.lowercase() ?: ""
            val date = item["date"] ?: ""

            val matchesSearch = title.contains(query.lowercase()) || message.contains(query.lowercase())
            val matchesDate = dateFilter == null || date.contains(dateFilter)

            if (matchesSearch && matchesDate) {
                filtered.add(item)
            }
        }
        adapter.updateList(filtered)
        updateEmptyState(filtered)
    }

    private fun updateEmptyState(list: ArrayList<Map<String, String>>) {
        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }
}