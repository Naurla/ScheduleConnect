package com.example.scheduleconnect

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Base64 // Import Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// Note: Changed type from ArrayList<Schedule> to List<Schedule> for better compatibility with filtering
class HistoryAdapter(private var scheduleList: List<Schedule>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var onItemClick: ((Schedule) -> Unit)? = null

    // --- NEW: FUNCTION TO UPDATE THE LIST FOR FILTERING ---
    fun updateList(newScheduleList: List<Schedule>) {
        scheduleList = newScheduleList
        notifyDataSetChanged()
    }
    // ------------------------------------------------------

    fun setOnItemClickListener(listener: (Schedule) -> Unit) {
        onItemClick = listener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvHistoryTitle)
        val date: TextView = view.findViewById(R.id.tvHistoryDate)
        val status: TextView = view.findViewById(R.id.tvHistoryStatus)
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

        // --- FIX: Decode Base64 String to Bitmap ---
        if (item.imageUrl.isNotEmpty()) {
            try {
                // 1. Convert Base64 String back to Bytes
                val decodedString = Base64.decode(item.imageUrl, Base64.DEFAULT)
                // 2. Convert Bytes to Bitmap (Image)
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                holder.image.visibility = View.VISIBLE
                holder.image.setImageBitmap(decodedByte)
                holder.image.scaleType = ImageView.ScaleType.CENTER_CROP

            } catch (e: Exception) {
                // If conversion fails, hide image or show placeholder
                holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.image.visibility = View.GONE
        }
        // ------------------------------------------

        // Dynamic Badge Color
        val bgShape = GradientDrawable()
        bgShape.shape = GradientDrawable.RECTANGLE
        bgShape.cornerRadius = 20f

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