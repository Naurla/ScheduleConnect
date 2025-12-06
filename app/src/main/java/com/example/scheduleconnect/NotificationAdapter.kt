package com.example.scheduleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private var list: ArrayList<Map<String, String>>) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvNotifTitle)
        val message: TextView = v.findViewById(R.id.tvNotifMessage)
        val date: TextView = v.findViewById(R.id.tvNotifDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item["title"]
        holder.message.text = item["message"]
        holder.date.text = item["date"]
    }

    override fun getItemCount() = list.size

    fun updateList(newList: ArrayList<Map<String, String>>) {
        list = newList
        notifyDataSetChanged()
    }
}