package com.example.scheduleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(private val list: ArrayList<Schedule>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvScheduleTitle)
        val date: TextView = v.findViewById(R.id.tvScheduleDate)
        val location: TextView = v.findViewById(R.id.tvScheduleLocation)
        val creator: TextView = v.findViewById(R.id.tvScheduleCreator) // New View
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item.title
        holder.date.text = item.date
        holder.location.text = item.location

        // Show creator only for shared schedules
        if (item.type == "shared" && item.creator.isNotEmpty()) {
            holder.creator.visibility = View.VISIBLE
            holder.creator.text = "Created by: ${item.creator}"
        } else {
            holder.creator.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size
}