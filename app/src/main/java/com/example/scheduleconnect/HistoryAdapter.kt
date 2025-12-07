package com.example.scheduleconnect

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        // ADDED: Reference to the ImageView
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

        // --- ADDED: Logic to display image ---
        if (item.image != null && item.image.isNotEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(item.image, 0, item.image.size)
                holder.image.setImageBitmap(bitmap)
                holder.image.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.image.visibility = View.GONE
            }
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