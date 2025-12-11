package com.example.scheduleconnect

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: HistoryAdapter

    // --- NEW: FILTERING VIEWS AND DATA ---
    private lateinit var etSearchHistory: EditText
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnClearFilters: Button

    private var allSchedules: List<Schedule> = emptyList() // Holds the full, unsorted list
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val scheduleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    // --- END NEW ---

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerHistory)
        layoutEmpty = view.findViewById(R.id.layoutEmptyHistory)
        tvEmpty = view.findViewById(R.id.tvEmptyHistory)

        // --- NEW: INITIALIZE FILTERING VIEWS ---
        etSearchHistory = view.findViewById(R.id.etSearchHistory)
        btnStartDate = view.findViewById(R.id.btnStartDate)
        btnEndDate = view.findViewById(R.id.btnEndDate)
        btnClearFilters = view.findViewById(R.id.btnClearFilters)

        // --- NEW: SETUP LISTENERS ---
        setupSearchListener()
        setupDateListeners()
        btnClearFilters.setOnClickListener { clearFilters() }
        // --- END NEW ---

        recyclerView.layoutManager = LinearLayoutManager(context)

        loadHistory()

        return view
    }

    // --- NEW: SEARCH LISTENER FUNCTION ---
    private fun setupSearchListener() {
        etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                updateClearButtonVisibility()
                filterSchedules()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // --- NEW: DATE LISTENER FUNCTION ---
    private fun setupDateListeners() {
        btnStartDate.setOnClickListener {
            showDatePicker(it) { date ->
                startDate = date
                btnStartDate.text = displayDateFormat.format(date)
                updateClearButtonVisibility()
                filterSchedules()
            }
        }
        btnEndDate.setOnClickListener {
            showDatePicker(it) { date ->
                endDate = date
                btnEndDate.text = displayDateFormat.format(date)
                updateClearButtonVisibility()
                filterSchedules()
            }
        }
    }

    // --- NEW: DATE PICKER DIALOG FUNCTION ---
    private fun showDatePicker(view: View, onDateSet: (Date) -> Unit) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)

            // Set time to start (00:00:00) or end (23:59:59) of the selected day
            if (view.id == R.id.btnStartDate) {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
            }
            onDateSet(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    // --- NEW: CORE FILTERING LOGIC FUNCTION ---
    private fun filterSchedules() {
        val query = etSearchHistory.text.toString().trim().lowercase(Locale.getDefault())

        val filteredList = allSchedules.filter { schedule ->
            // 1. Search Filter (Title/Creator)
            val matchesSearch = if (query.isEmpty()) true else
                schedule.title.lowercase(Locale.getDefault()).contains(query) ||
                        schedule.creator.lowercase(Locale.getDefault()).contains(query)

            // 2. Date Filter
            val matchesDate = try {
                val scheduleDate = scheduleDateFormat.parse(schedule.date)

                // Check if the schedule date is ON or AFTER the start date
                val matchesStart = startDate == null || (scheduleDate != null && !scheduleDate.before(startDate))
                // Check if the schedule date is ON or BEFORE the end date
                val matchesEnd = endDate == null || (scheduleDate != null && !scheduleDate.after(endDate))

                matchesStart && matchesEnd
            } catch (e: Exception) {
                // If date parsing fails, exclude the schedule
                false
            }

            matchesSearch && matchesDate
        }

        // Update UI based on filtered results
        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (query.isNotEmpty() || startDate != null || endDate != null) {
                "No schedules found matching your filters."
            } else {
                "No history yet"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            // Use the adapter function to update the list
            adapter.updateList(filteredList.sortedByDescending { it.id })
        }
    }

    // --- NEW: CLEAR FILTERS FUNCTION ---
    private fun clearFilters() {
        startDate = null
        endDate = null
        etSearchHistory.setText("") // This automatically triggers filterSchedules via the TextWatcher
        btnStartDate.text = "START DATE"
        btnEndDate.text = "END DATE"
        btnClearFilters.visibility = View.GONE
        filterSchedules() // Although setText() calls it, this is a safeguard
    }

    // --- NEW: UPDATE CLEAR BUTTON VISIBILITY ---
    private fun updateClearButtonVisibility() {
        val isFiltering = etSearchHistory.text.isNotEmpty() || startDate != null || endDate != null
        btnClearFilters.visibility = if (isFiltering) View.VISIBLE else View.GONE
    }

    // --- MODIFIED loadHistory() ---
    private fun loadHistory() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        dbHelper.getHistorySchedules(currentUser) { historyList ->
            // --- MODIFICATION: SAVE THE FULL LIST ---
            allSchedules = historyList.toList()

            if (historyList.isEmpty()) {
                recyclerView.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE

                // Initial load: sort and display the full list
                val sortedList = historyList.sortedByDescending { it.id }

                adapter = HistoryAdapter(sortedList)
                recyclerView.adapter = adapter

                adapter.setOnItemClickListener { schedule ->
                    openScheduleDetails(schedule)
                }
            }
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
        bundle.putString("SCH_IMAGE", schedule.imageUrl)

        bundle.putBoolean("IS_FROM_HISTORY", true)

        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}

// NOTE: You must ensure your HistoryAdapter has this function:
/*
class HistoryAdapter(private var schedules: List<Schedule>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    // ... existing adapter code ...

    fun updateList(newSchedules: List<Schedule>) {
        schedules = newSchedules
        notifyDataSetChanged()
    }

    // ... existing adapter code ...
}
*/