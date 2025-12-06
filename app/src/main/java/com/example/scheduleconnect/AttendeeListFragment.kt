package com.example.scheduleconnect

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AttendeeListFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    // Counter Views
    private lateinit var tvCountGoing: TextView
    private lateinit var tvCountUnsure: TextView
    private lateinit var tvCountNotGoing: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_attendee_list, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. Arguments
        val schId = arguments?.getInt("SCH_ID") ?: -1
        val title = arguments?.getString("SCH_TITLE") ?: "Schedule"
        val creator = arguments?.getString("SCH_CREATOR") ?: "Unknown"

        // 2. Setup Header
        view.findViewById<TextView>(R.id.tvAttendeeTitle).text = title
        view.findViewById<TextView>(R.id.tvCreatorName).text = "Schedule by: $creator"

        view.findViewById<ImageView>(R.id.btnBackAttendee).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 3. Init Views
        recyclerView = view.findViewById(R.id.recyclerAttendees)
        tvEmpty = view.findViewById(R.id.tvEmptyAttendees)
        tvCountGoing = view.findViewById(R.id.tvCountGoing)
        tvCountUnsure = view.findViewById(R.id.tvCountUnsure)
        tvCountNotGoing = view.findViewById(R.id.tvCountNotGoing)

        recyclerView.layoutManager = LinearLayoutManager(context)

        loadAttendees(schId)

        return view
    }

    private fun loadAttendees(schId: Int) {
        val list = dbHelper.getScheduleAttendees(schId)

        // --- NEW: Calculate Counts ---
        var goingCount = 0
        var unsureCount = 0
        var notGoingCount = 0

        for (item in list) {
            when (item["status"]) {
                "GOING" -> goingCount++
                "UNSURE" -> unsureCount++
                "NOT GOING" -> notGoingCount++
            }
        }

        // Update Counter Text
        tvCountGoing.text = "GOING: $goingCount"
        tvCountUnsure.text = "UNSURE: $unsureCount"
        tvCountNotGoing.text = "NOT GOING: $notGoingCount"

        // --- Populate List ---
        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            recyclerView.adapter = AttendeeAdapter(list)
        }
    }
}

class AttendeeAdapter(private val list: ArrayList<Map<String, String>>) : RecyclerView.Adapter<AttendeeAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val user: TextView = v.findViewById(R.id.tvAttUser)
        val status: TextView = v.findViewById(R.id.tvAttStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.user.text = item["username"]?.uppercase()

        when(item["status"]) {
            "GOING" -> {
                holder.status.text = "I WILL ATTEND"
                holder.status.setTextColor(Color.parseColor("#388E3C")) // Green
            }
            "NOT GOING" -> {
                holder.status.text = "I WILL NOT ATTEND"
                holder.status.setTextColor(Color.parseColor("#D32F2F")) // Red
            }
            else -> {
                holder.status.text = "UNSURE"
                holder.status.setTextColor(Color.parseColor("#F57C00")) // Orange
            }
        }
    }

    override fun getItemCount() = list.size
}