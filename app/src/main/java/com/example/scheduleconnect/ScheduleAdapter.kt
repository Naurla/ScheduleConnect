package com.example.scheduleconnect

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(private val list: ArrayList<Schedule>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var listener: ((Schedule) -> Unit)? = null

    fun setOnItemClickListener(listener: (Schedule) -> Unit) {
        this.listener = listener
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgHeader: ImageView = v.findViewById(R.id.imgScheduleHeader)
        val title: TextView = v.findViewById(R.id.tvScheduleTitle)
        val date: TextView = v.findViewById(R.id.tvScheduleDate)
        val loc: TextView = v.findViewById(R.id.tvScheduleLocation)
        val creator: TextView = v.findViewById(R.id.tvScheduleCreator)
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

        // --- CRITICAL FIX: Safe Image Loading for List ---
        if (item.image != null && item.image.isNotEmpty()) {
            try {
                // 1. Decode dimensions only
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(item.image, 0, item.image.size, options)

                // 2. Calculate scale (shrink to ~200px for the list view)
                options.inSampleSize = calculateInSampleSize(options, 200, 200)

                // 3. Decode actual image
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeByteArray(item.image, 0, item.image.size, options)

                holder.imgHeader.setImageBitmap(bitmap)
                holder.imgHeader.visibility = View.VISIBLE
            } catch (e: Exception) {
                // If image is corrupt or too big, hide it instead of crashing
                holder.imgHeader.visibility = View.GONE
            }
        } else {
            holder.imgHeader.visibility = View.GONE
        }
        // -----------------------------------------------

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

    // Helper to calculate shrinkage
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}