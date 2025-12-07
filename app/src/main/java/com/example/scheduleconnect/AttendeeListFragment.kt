package com.example.scheduleconnect

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AttendeeListFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var tvCountGoing: TextView
    private lateinit var tvCountUnsure: TextView
    private lateinit var tvCountNotGoing: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_attendee_list, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val schId = arguments?.getInt("SCH_ID") ?: -1
        val title = arguments?.getString("SCH_TITLE") ?: "Schedule"
        val creator = arguments?.getString("SCH_CREATOR") ?: "Unknown"

        view.findViewById<TextView>(R.id.tvAttendeeTitle).text = title
        view.findViewById<TextView>(R.id.tvCreatorName).text = "Schedule by: $creator"

        view.findViewById<ImageView>(R.id.btnBackAttendee).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recyclerAttendees)
        tvEmpty = view.findViewById(R.id.tvEmptyAttendees)
        tvCountGoing = view.findViewById(R.id.tvCountGoing)
        tvCountUnsure = view.findViewById(R.id.tvCountUnsure)
        tvCountNotGoing = view.findViewById(R.id.tvCountNotGoing)

        recyclerView.layoutManager = LinearLayoutManager(context)

        loadAttendees(schId)

        return view
    }

    private fun loadAttendees(schId: Int) {
        val list = dbHelper.getScheduleAttendees(schId)

        var goingCount = 0
        var unsureCount = 0
        var notGoingCount = 0

        for (item in list) {
            when (item["status"]) {
                "GOING" -> goingCount++
                "UNSURE" -> unsureCount++
                "NOT GOING" -> notGoingCount++
            }
        }

        // Only show numbers
        tvCountGoing.text = goingCount.toString()
        tvCountUnsure.text = unsureCount.toString()
        tvCountNotGoing.text = notGoingCount.toString()

        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            // Pass dbHelper to adapter to load images
            recyclerView.adapter = AttendeeAdapter(list, dbHelper)
        }
    }
}

class AttendeeAdapter(
    private val list: ArrayList<Map<String, String>>,
    private val dbHelper: DatabaseHelper
) : RecyclerView.Adapter<AttendeeAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // FIXED IDs to match item_attendee.xml
        val user: TextView = v.findViewById(R.id.tvAttendeeName)
        val status: TextView = v.findViewById(R.id.tvAttendeeStatus)
        val avatar: ImageView = v.findViewById(R.id.ivAttendeeAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendee, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val username = item["username"] ?: "Unknown"

        holder.user.text = username

        when(item["status"]) {
            "GOING" -> {
                holder.status.text = "Going"
                holder.status.setTextColor(Color.parseColor("#388E3C"))
            }
            "NOT GOING" -> {
                holder.status.text = "Not Going"
                holder.status.setTextColor(Color.parseColor("#D32F2F"))
            }
            else -> {
                holder.status.text = "Unsure"
                holder.status.setTextColor(Color.parseColor("#F57C00"))
            }
        }

        // Load Profile Picture
        val bmp = dbHelper.getProfilePicture(username)
        if (bmp != null) {
            holder.avatar.setImageBitmap(bmp)
            holder.avatar.imageTintList = null // Remove gray tint for real photo
        } else {
            holder.avatar.setImageResource(R.drawable.ic_person)
            holder.avatar.setColorFilter(Color.parseColor("#999999"))
        }
    }

    override fun getItemCount() = list.size
}