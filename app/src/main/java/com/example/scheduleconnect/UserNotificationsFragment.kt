package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class UserNotificationsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var adapter: NotificationAdapter
    private var currentUser: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user_notifications, container, false)
        dbHelper = DatabaseHelper(requireContext())

        recyclerView = view.findViewById(R.id.recyclerUserNotifications)
        layoutEmpty = view.findViewById(R.id.layoutEmptyNotifs)
        btnBack = view.findViewById(R.id.btnBackUserNotif)

        // --- FIX: Use "username" (lowercase) to match HomeActivity ---
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "") ?: ""

        setupRecyclerView()

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadNotifications()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(ArrayList(),
            onMarkReadClick = { item ->
                dbHelper.markNotificationRead(item.id)
                loadNotifications() // Refresh list
                // Update badge in parent Activity
                (activity as? HomeActivity)?.updateNotificationBadge()
            },
            onItemClick = { item ->
                // --- FIX: Use 'read' property ---
                if (!item.read) {
                    dbHelper.markNotificationRead(item.id)
                    (activity as? HomeActivity)?.updateNotificationBadge()
                }

                // Redirect Logic
                if (item.type == "SCHEDULE" && item.relatedId != -1) {
                    val fragment = ScheduleDetailFragment()
                    val bundle = Bundle()
                    bundle.putInt("SCH_ID", item.relatedId)
                    fragment.arguments = bundle

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                } else if (item.type == "GROUP" && item.relatedId != -1) {
                    val fragment = GroupDetailsFragment()
                    val bundle = Bundle()
                    bundle.putInt("GROUP_ID", item.relatedId)
                    fragment.arguments = bundle

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadNotifications() {
        if (currentUser.isNotEmpty()) {
            dbHelper.getUserNotifications(currentUser) { list ->
                if (list.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmpty.visibility = View.GONE

                    // Sort: Newest first
                    list.sortByDescending { it.id }

                    adapter.updateList(list)
                }
            }
        }
    }
}
