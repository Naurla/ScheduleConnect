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

class ChatAdapter(
    private val context: Context,
    private val messages: List<ChatMessage>,
    private val currentUser: String,
    private val groupId: Int,
    private val dbHelper: DatabaseHelper
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // Cache to store fetched nicknames so we don't call DB for every message
    private val nicknameCache = HashMap<String, String>()

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

            // --- NICKNAME LOGIC ---
            // Check if we already have the nickname in cache
            if (nicknameCache.containsKey(msg.sender)) {
                val cachedName = nicknameCache[msg.sender]
                holder.tvSender.text = if (cachedName.isNullOrEmpty()) msg.sender else cachedName
            } else {
                // Show username temporarily
                holder.tvSender.text = msg.sender

                // Fetch nickname from DB
                dbHelper.getGroupMemberNickname(groupId, msg.sender) { nickname ->
                    // Store in cache (even if empty, to avoid refetching)
                    nicknameCache[msg.sender] = nickname

                    // Update UI if nickname found
                    if (nickname.isNotEmpty()) {
                        holder.tvSender.text = nickname
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
