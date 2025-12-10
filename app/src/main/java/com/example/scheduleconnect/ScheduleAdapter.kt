package com.example.scheduleconnect

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(private var list: ArrayList<Schedule>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var listener: ((Schedule) -> Unit)? = null

    fun setOnItemClickListener(listener: (Schedule) -> Unit) {
        this.listener = listener
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvScheduleTitle)
        val date: TextView = v.findViewById(R.id.tvScheduleDate)
        val loc: TextView = v.findViewById(R.id.tvScheduleLocation)
        val creator: TextView = v.findViewById(R.id.tvScheduleCreator)
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

        // --- UPDATED: BASE64 IMAGE DECODING (NO GLIDE) ---
        if (item.imageUrl.isNotEmpty()) {
            try {
                // 1. Decode the Base64 string to bytes
                val decodedString = Base64.decode(item.imageUrl, Base64.DEFAULT)
                // 2. Turn bytes into a Bitmap
                val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                // 3. Set Bitmap to ImageView
                holder.image.setImageBitmap(decodedByte)
                holder.image.visibility = View.VISIBLE

                // Remove padding/tint if it was a placeholder
                holder.image.setPadding(0,0,0,0)
                holder.image.colorFilter = null

            } catch (e: Exception) {
                // If decoding fails, hide the image or show a placeholder
                holder.image.visibility = View.GONE
                e.printStackTrace()
            }
        } else {
            holder.image.visibility = View.GONE
        }
        // ------------------------------------------------

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

    fun updateList(newList: ArrayList<Schedule>) {
        list = newList
        notifyDataSetChanged()
    }
}