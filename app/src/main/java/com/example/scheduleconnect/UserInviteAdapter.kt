package com.example.scheduleconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserInviteAdapter(
    private var userList: ArrayList<String>,
    private val onInviteClick: (String) -> Unit
) : RecyclerView.Adapter<UserInviteAdapter.ViewHolder>() {

    // Keep track of invited users to change UI state
    private val invitedUsers = ArrayList<String>()

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val username: TextView = v.findViewById(R.id.tvInviteUsername)
        val btnInvite: TextView = v.findViewById(R.id.btnInviteAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_invite, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.username.text = user

        // Change text if already invited
        if (invitedUsers.contains(user)) {
            holder.btnInvite.text = "ADDED"
            holder.btnInvite.setTextColor(android.graphics.Color.GRAY)
            holder.btnInvite.isEnabled = false
        } else {
            holder.btnInvite.text = "INVITE +"
            holder.btnInvite.setTextColor(android.graphics.Color.parseColor("#8B1A1A"))
            holder.btnInvite.isEnabled = true
        }

        holder.btnInvite.setOnClickListener {
            if (!invitedUsers.contains(user)) {
                onInviteClick(user)
                invitedUsers.add(user)
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = userList.size

    fun updateList(newList: ArrayList<String>) {
        userList = newList
        notifyDataSetChanged()
    }
}