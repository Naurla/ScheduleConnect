package com.example.scheduleconnect

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Import Glide

class HistoryAdapter(private val scheduleList: ArrayList<Schedule>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var onItemClick: ((Schedule) -> Unit)? = null

    fun setOnItemClickListener(listener: (Schedule) -> Unit) {
        onItemClick = listener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvHistoryTitle)
        val date: TextView = view.findViewById(R.id.tvHistoryDate)
        val status: TextView = view.findViewById(R.id.tvHistoryStatus)
        // Reference to the ImageView
        val image: ImageView = view.findViewById(R.id.ivHistoryImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = scheduleList[position]

        holder.title.text = item.title
        holder.date.text = item.date
        holder.status.text = item.status

        // --- UPDATED: Glide for Image URLs ---
        // We now check 'imageUrl' string instead of 'image' byte array
        if (item.imageUrl.isNotEmpty()) {
            holder.image.visibility = View.VISIBLE

            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder while loading
                .centerCrop()
                .into(holder.image)
        } else {
            holder.image.visibility = View.GONE
        }
        // -------------------------------------

        // Dynamic Badge Color
        val bgShape = GradientDrawable()
        bgShape.shape = GradientDrawable.RECTANGLE
        bgShape.cornerRadius = 20f // Rounded corners for pill shape

        if (item.status == "FINISHED") {
            bgShape.setColor(Color.parseColor("#8B1A1A")) // App Red
        } else {
            bgShape.setColor(Color.parseColor("#757575")) // Gray for Cancelled
        }

        holder.status.background = bgShape

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return scheduleList.size
    }
}