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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_attendee_list, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val schId = arguments?.getInt("SCH_ID") ?: -1
        val title = arguments?.getString("SCH_TITLE") ?: "Attendees"

        view.findViewById<TextView>(R.id.tvAttendeeTitle).text = title
        view.findViewById<ImageView>(R.id.btnBackAttendee).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recyclerAttendees)
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadAttendees(schId)

        return view
    }

    private fun loadAttendees(schId: Int) {
        val list = dbHelper.getScheduleAttendees(schId)
        recyclerView.adapter = AttendeeAdapter(list)
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
        holder.user.text = item["username"]
        val status = item["status"]
        holder.status.text = status

        when(status) {
            "GOING" -> holder.status.setTextColor(Color.parseColor("#388E3C")) // Green
            "UNSURE" -> holder.status.setTextColor(Color.parseColor("#F57C00")) // Orange
            "NOT GOING" -> holder.status.setTextColor(Color.parseColor("#D32F2F")) // Red
        }
    }

    override fun getItemCount() = list.size
}