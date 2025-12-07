package com.example.scheduleconnect

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Changed 'val' to 'var' so we can update the list
class ScheduleAdapter(private var list: ArrayList<Schedule>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var listener: ((Schedule) -> Unit)? = null

    fun setOnItemClickListener(listener: (Schedule) -> Unit) {
        this.listener = listener
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // R.id.tvScheduleTitle is correctly referenced here, provided the R class is valid.
        val title: TextView = v.findViewById(R.id.tvScheduleTitle)
        val date: TextView = v.findViewById(R.id.tvScheduleDate)
        val loc: TextView = v.findViewById(R.id.tvScheduleLocation)
        val creator: TextView = v.findViewById(R.id.tvScheduleCreator)
        // ADDED: Reference to the ImageView
        val image: ImageView = v.findViewById(R.id.ivScheduleImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item.title
        holder.date.text = item.date
        holder.loc.text = item.location

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

        if (item.type == "shared") {
            holder.creator.text = "By: ${item.creator}"
            holder.creator.visibility = View.VISIBLE
            holder.title.setTextColor(Color.parseColor("#8B1A1A"))
        } else {
            holder.creator.visibility = View.GONE
            holder.title.setTextColor(Color.parseColor("#8B1A1A"))
        }

        holder.itemView.setOnClickListener {
            listener?.invoke(item)
        }
    }

    override fun getItemCount() = list.size

    // --- NEW: Helper function to update data for search ---
    fun updateList(newList: ArrayList<Schedule>) {
        list = newList
        notifyDataSetChanged()
    }
}