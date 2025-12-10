package com.example.scheduleconnect

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// REMOVED: data class ChatMessage(...) because it is already in DatabaseHelper.kt

class ChatAdapter(
    private val context: Context,
    private val messages: List<ChatMessage>,
    private val currentUser: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutContainer: LinearLayout = view.findViewById(R.id.chatLayoutContainer)
        val cardView: CardView = view.findViewById(R.id.chatCard)
        val tvSender: TextView = view.findViewById(R.id.tvChatSender)
        val tvMessage: TextView = view.findViewById(R.id.tvChatMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]

        holder.tvMessage.text = msg.message
        holder.tvSender.text = msg.sender

        if (msg.sender == currentUser) {
            // My Message (Align Right, Red Color)
            holder.layoutContainer.gravity = Gravity.END
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.app_red))
            holder.tvMessage.setTextColor(Color.WHITE)
            holder.tvSender.visibility = View.GONE
        } else {
            // Other's Message (Align Left, Gray Color)
            holder.layoutContainer.gravity = Gravity.START
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
            holder.tvMessage.setTextColor(Color.BLACK)
            holder.tvSender.visibility = View.VISIBLE
            holder.tvSender.text = msg.sender
        }
    }

    override fun getItemCount(): Int = messages.size
}