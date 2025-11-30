package com.example.scheduleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Random

class HistoryAdapter(private var list: ArrayList<Schedule>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val date: TextView = v.findViewById(R.id.tvHistoryDate)
        val title: TextView = v.findViewById(R.id.tvHistoryTitle)
        val location: TextView = v.findViewById(R.id.tvHistoryLocation)
        val status: TextView = v.findViewById(R.id.tvStatus)
        val sharedInfo: TextView = v.findViewById(R.id.tvHistorySharedBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.title.text = item.title
        holder.date.text = item.date
        holder.location.text = item.location

        // Fake Logic to simulate the UI statuses (since we don't have full logic for "Did not attend" yet)
        if (position % 2 == 0) {
            holder.status.text = "DONE"
        } else {
            holder.status.text = "DID NOT\nATTEND"
        }

        // Show extra info if it's a shared schedule
        if (item.type == "shared") {
            holder.sharedInfo.visibility = View.VISIBLE
            holder.sharedInfo.text = "Type: Shared Group Event"
        } else {
            holder.sharedInfo.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size

    // Helper to update list for search
    fun updateList(newList: ArrayList<Schedule>) {
        list = newList
        notifyDataSetChanged()
    }
}