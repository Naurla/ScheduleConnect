package com.example.scheduleconnect

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// FIX: This class MUST accept 'ArrayList<NotificationItem>'
class NotificationAdapter(
    private var list: ArrayList<NotificationItem>,
    private val onMarkReadClick: (NotificationItem) -> Unit,
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val root: LinearLayout = v.findViewById(R.id.llNotificationRoot)
        val title: TextView = v.findViewById(R.id.tvNotifTitle)
        val msg: TextView = v.findViewById(R.id.tvNotifMessage)
        val date: TextView = v.findViewById(R.id.tvNotifDate)
        val markRead: TextView = v.findViewById(R.id.tvMarkAsRead)
        val unreadDot: View = v.findViewById(R.id.viewUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.title.text = item.title
        holder.msg.text = item.message
        holder.date.text = item.date

        // Logic: Show/Hide "Mark as Read" based on status
        if (item.isRead) {
            holder.root.setBackgroundColor(Color.WHITE)
            holder.title.setTypeface(null, Typeface.NORMAL)
            holder.markRead.visibility = View.GONE
            holder.unreadDot.visibility = View.GONE
        } else {
            holder.root.setBackgroundColor(Color.parseColor("#FFF5F5")) // Light red tint
            holder.title.setTypeface(null, Typeface.BOLD)
            holder.markRead.visibility = View.VISIBLE
            holder.unreadDot.visibility = View.VISIBLE
        }

        holder.markRead.setOnClickListener { onMarkReadClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: ArrayList<NotificationItem>) {
        list = newList
        notifyDataSetChanged()
    }
}