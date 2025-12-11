package com.example.scheduleconnect

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    private lateinit var btnOpenDateFilter: ImageView // Header calendar icon
    private lateinit var tvDateRangeSummary: TextView // Replaces subtitle for date summary

    private var allSchedules: List<Schedule> = emptyList()
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val scheduleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    // --- END NEW ---

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerHistory) // <-- Line of potential error resolved by ensuring correct file state
        layoutEmpty = view.findViewById(R.id.layoutEmptyHistory)
        tvEmpty = view.findViewById(R.id.tvEmptyHistory)

        // --- NEW: INITIALIZE FILTERING VIEWS ---
        etSearchHistory = view.findViewById(R.id.etSearchHistory)
        btnOpenDateFilter = view.findViewById(R.id.btnOpenDateFilter)
        tvDateRangeSummary = view.findViewById(R.id.tvDateRangeSummary)

        // --- NEW: SETUP LISTENERS ---
        setupSearchListener()
        setupDateFilterTrigger()
        updateDateSummary()
        // --- END NEW ---

        recyclerView.layoutManager = LinearLayoutManager(context)

        loadHistory()

        return view
    }

    private fun setupDateFilterTrigger() {
        btnOpenDateFilter.setOnClickListener {
            showDateRangeModal()
        }
    }

    private fun updateDateSummary() {
        tvDateRangeSummary.text = if (startDate != null || endDate != null) {
            val startText = if (startDate != null) displayDateFormat.format(startDate!!) else "Start"
            val endText = if (endDate != null) displayDateFormat.format(endDate!!) else "End"
            "Filtering: $startText - $endText"
        } else {
            "Your past schedules."
        }
    }

    // --- NEW: MODAL LOGIC ---
    private fun showDateRangeModal() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_date_range_options, null)

        val btnModalStartDate = dialogView.findViewById<Button>(R.id.btnModalStartDate)
        val btnModalEndDate = dialogView.findViewById<Button>(R.id.btnModalEndDate)
        val btnModalClearFilter = dialogView.findViewById<Button>(R.id.btnModalClearFilter)

        // Update button text to reflect current selection
        btnModalStartDate.text = startDate?.let { displayDateFormat.format(it) } ?: "Start Date"
        btnModalEndDate.text = endDate?.let { displayDateFormat.format(it) } ?: "End Date"

        // Show clear button if any filter is active
        if (startDate != null || endDate != null) {
            btnModalClearFilter.visibility = View.VISIBLE
        } else {
            btnModalClearFilter.visibility = View.GONE
        }

        builder.setView(dialogView)
        val dialog = builder.create()
        // Ensure you have bg_rounded_red.xml and dialog_date_range_options.xml in your resources
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // --- Click Handlers for the Modal Buttons ---

        btnModalStartDate.setOnClickListener {
            dialog.dismiss()
            showDatePicker(it) { date ->
                startDate = date
                updateDateSummary()
                filterSchedules()
            }
        }

        btnModalEndDate.setOnClickListener {
            dialog.dismiss()
            showDatePicker(it) { date ->
                endDate = date
                updateDateSummary()
                filterSchedules()
            }
        }

        btnModalClearFilter.setOnClickListener {
            clearDateFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    // --- MODIFIED: Date Picker Dialog (Called from Modal) ---
    private fun showDatePicker(triggerView: View, onDateSet: (Date) -> Unit) {
        val calendar = Calendar.getInstance()

        val initialDate = when (triggerView.id) {
            R.id.btnModalStartDate -> startDate
            R.id.btnModalEndDate -> endDate
            else -> null
        }
        initialDate?.let { calendar.time = it }

        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)

            if (triggerView.id == R.id.btnModalStartDate) {
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

    // --- NEW: CLEAR DATE FILTERS FUNCTION (Used by Modal) ---
    private fun clearDateFilters() {
        startDate = null
        endDate = null
        updateDateSummary()
        filterSchedules()
    }


    // --- Other Fragment functions ---
    private fun setupSearchListener() {
        etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                filterSchedules()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterSchedules() {
        val query = etSearchHistory.text.toString().trim().lowercase(Locale.getDefault())

        val filteredList = allSchedules.filter { schedule ->
            val matchesSearch = if (query.isEmpty()) true else
                schedule.title.lowercase(Locale.getDefault()).contains(query) ||
                        schedule.creator.lowercase(Locale.getDefault()).contains(query)

            val matchesDate = try {
                val scheduleDate = scheduleDateFormat.parse(schedule.date)

                val matchesStart = startDate == null || (scheduleDate != null && !scheduleDate.before(startDate))
                val matchesEnd = endDate == null || (scheduleDate != null && !scheduleDate.after(endDate))

                matchesStart && matchesEnd
            } catch (e: Exception) {
                false
            }

            matchesSearch && matchesDate
        }

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
            adapter.updateList(filteredList.sortedByDescending { it.id })
        }
    }

    private fun loadHistory() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        dbHelper.getHistorySchedules(currentUser) { historyList ->
            allSchedules = historyList.toList()

            if (historyList.isEmpty()) {
                recyclerView.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE

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