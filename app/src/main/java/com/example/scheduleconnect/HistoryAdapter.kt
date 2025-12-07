package com.example.scheduleconnect

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private var list: ArrayList<Schedule>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // --- NEW: Listener for click events ---
    private var listener: ((Schedule) -> Unit)? = null

    fun setOnItemClickListener(listener: (Schedule) -> Unit) {
        this.listener = listener
    }

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

        // Show real status
        when (item.status) {
            "FINISHED" -> {
                holder.status.text = "FINISHED"
                holder.status.setTextColor(Color.parseColor("#388E3C")) // Green
            }
            "CANCELLED" -> {
                holder.status.text = "CANCELLED"
                holder.status.setTextColor(Color.parseColor("#D32F2F")) // Red
            }
            else -> {
                holder.status.text = "DONE"
                holder.status.setTextColor(Color.GRAY)
            }
        }

        if (item.type == "shared") {
            holder.sharedInfo.visibility = View.VISIBLE
            holder.sharedInfo.text = "Shared by: ${item.creator}"
        } else {
            holder.sharedInfo.visibility = View.GONE
        }

        // --- NEW: Handle Click ---
        holder.itemView.setOnClickListener {
            listener?.invoke(item)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: ArrayList<Schedule>) {
        list = newList
        notifyDataSetChanged()
    }
}