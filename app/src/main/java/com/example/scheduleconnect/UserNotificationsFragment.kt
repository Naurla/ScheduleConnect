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

        // --- MATCHING IDs FROM YOUR XML ---
        recyclerView = view.findViewById(R.id.recyclerUserNotifications)
        layoutEmpty = view.findViewById(R.id.layoutEmptyNotifs)
        btnBack = view.findViewById(R.id.btnBackUserNotif)

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "") ?: ""

        setupRecyclerView()

        // Handle Back Button
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadNotifications()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize Adapter with Empty List and the required Click Listeners
        adapter = NotificationAdapter(ArrayList(),
            onMarkReadClick = { item ->
                // When "Mark as Read" is clicked
                dbHelper.markNotificationRead(item.id)
                loadNotifications() // Refresh list
                (activity as? HomeActivity)?.updateNotificationBadge() // Update Top Badge
            },
            onItemClick = { item ->
                // When the card is clicked
                if (!item.isRead) {
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
                }
                // --- NEW BLOCK: Redirect to Group Details ---
                else if (item.type == "GROUP" && item.relatedId != -1) {
                    val fragment = GroupDetailsFragment()
                    val bundle = Bundle()
                    bundle.putInt("GROUP_ID", item.relatedId)
                    // Note: Name and Code will be fetched inside GroupDetailsFragment
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
            // --- ASYNC FETCH ---
            dbHelper.getUserNotifications(currentUser) { list ->
                if (list.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    layoutEmpty.visibility = View.GONE

                    // Sort by Date/ID (Newest first) - assuming ID is time-based
                    list.sortByDescending { it.id }

                    adapter.updateList(list)
                }
            }
        }
    }
}

