package com.example.scheduleconnect


import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class NotificationAdapter(
    private var notificationList: ArrayList<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit,
    private val onAcceptClick: (NotificationItem) -> Unit,
    private val onDeclineClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit // New Parameter
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val message: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val date: TextView = itemView.findViewById(R.id.tvNotificationDate)
        val icon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        val unreadDot: ImageView = itemView.findViewById(R.id.ivUnreadDot)


        // Actions
        val layoutActions: LinearLayout = itemView.findViewById(R.id.layoutInviteActions)
        val btnAccept: Button = itemView.findViewById(R.id.btnAcceptInvite)
        val btnDecline: Button = itemView.findViewById(R.id.btnDeclineInvite)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteNotification) // New
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notificationList[position]
        holder.title.text = notif.title
        holder.message.text = notif.message
        holder.date.text = notif.date


        if (notif.read) {
            holder.title.typeface = Typeface.DEFAULT
            holder.unreadDot.visibility = View.GONE
            holder.itemView.setBackgroundColor(Color.WHITE)
        } else {
            holder.title.typeface = Typeface.DEFAULT_BOLD
            holder.unreadDot.visibility = View.VISIBLE
            holder.itemView.setBackgroundColor(Color.parseColor("#FAFAFA"))
        }


        // Logic for Invite Buttons
        if (notif.type == "GROUP_INVITE") {
            holder.layoutActions.visibility = View.VISIBLE
            holder.icon.setImageResource(R.drawable.ic_group)
            holder.btnAccept.setOnClickListener { onAcceptClick(notif) }
            holder.btnDecline.setOnClickListener { onDeclineClick(notif) }
        } else {
            holder.layoutActions.visibility = View.GONE
            if (notif.title.contains("Security", true)) {
                holder.icon.setImageResource(R.drawable.ic_settings_security)
            } else {
                holder.icon.setImageResource(R.drawable.ic_settings_notifications)
            }
        }


        // Handle Manual Delete Button
        holder.btnDelete.setOnClickListener { onDeleteClick(notif) }


        // Standard Item Click
        holder.itemView.setOnClickListener { onItemClick(notif) }
    }


    override fun getItemCount() = notificationList.size


    fun updateList(newList: ArrayList<NotificationItem>) {
        notificationList = newList
        notifyDataSetChanged()
    }
}

