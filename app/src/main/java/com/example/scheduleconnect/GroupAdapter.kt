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
import com.bumptech.glide.Glide

class GroupAdapter(
    private var groupList: ArrayList<GroupInfo>,
    private val onItemClick: (GroupInfo) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // These IDs must match your item_group.xml
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvGroupCode: TextView = view.findViewById(R.id.tvGroupCode)
        val ivGroupImage: ImageView = view.findViewById(R.id.ivGroupImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groupList[position]
        holder.tvGroupName.text = group.name
        holder.tvGroupCode.text = group.code

        // --- IMAGE LOGIC (The Fix) ---
        if (group.imageUrl.isNotEmpty()) {

            // 1. Decode the image
            if (group.imageUrl.startsWith("http")) {
                // If it's an old URL style
                Glide.with(holder.itemView.context)
                    .load(group.imageUrl)
                    .centerCrop()
                    .into(holder.ivGroupImage)
            } else {
                // If it's the NEW Base64 Text style
                try {
                    val decodedString = Base64.decode(group.imageUrl, Base64.DEFAULT)
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    holder.ivGroupImage.setImageBitmap(decodedByte)
                    holder.ivGroupImage.scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (e: Exception) {
                    holder.ivGroupImage.setImageResource(R.drawable.ic_group)
                }
            }

            // 2. IMPORTANT: Remove the red tint and padding so the photo looks real
            holder.ivGroupImage.setPadding(0, 0, 0, 0)
            holder.ivGroupImage.imageTintList = null
            holder.ivGroupImage.colorFilter = null

        } else {
            // 3. If no image, show the default red/pink icon
            holder.ivGroupImage.setImageResource(R.drawable.ic_group)

            // Restore default styling (Red tint + Padding)
            holder.ivGroupImage.setColorFilter(Color.parseColor("#D32F2F"))
            val paddingDp = 12
            val density = holder.itemView.context.resources.displayMetrics.density
            val paddingPixel = (paddingDp * density).toInt()
            holder.ivGroupImage.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel)
        }

        holder.itemView.setOnClickListener { onItemClick(group) }
    }

    override fun getItemCount(): Int = groupList.size

    fun updateList(newList: ArrayList<GroupInfo>) {
        groupList = newList
        notifyDataSetChanged()
    }
}