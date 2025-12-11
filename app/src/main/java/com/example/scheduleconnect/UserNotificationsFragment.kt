package com.example.scheduleconnect


import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList


class UserNotificationsFragment : Fragment() {


    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyNotifs: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnMarkAllRead: TextView
    private lateinit var btnClearAll: TextView
    private lateinit var adapter: NotificationAdapter
    private var notificationList = ArrayList<NotificationItem>()
    private lateinit var currentUser: String


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user_notifications, container, false)
        dbHelper = DatabaseHelper(requireContext())


        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"


        recyclerView = view.findViewById(R.id.recyclerUserNotifications)
        layoutEmptyNotifs = view.findViewById(R.id.layoutEmptyNotifs)
        btnBack = view.findViewById(R.id.btnBackUserNotif)
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead)
        btnClearAll = view.findViewById(R.id.btnClearAllNotifs)


        recyclerView.layoutManager = LinearLayoutManager(context)


        adapter = NotificationAdapter(
            notificationList,
            onItemClick = { notif ->
                dbHelper.markNotificationRead(notif.id)
                loadNotifications()


                if (notif.type == "CHAT" && notif.relatedId != -1) {
                    val chatFragment = GroupChatFragment()
                    val bundle = Bundle()
                    bundle.putInt("GROUP_ID", notif.relatedId)
                    bundle.putString("GROUP_NAME", notif.groupName)
                    chatFragment.arguments = bundle
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, chatFragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onAcceptClick = { notif -> acceptInvite(notif) },
            onDeclineClick = { notif ->
                // Using the same confirmation for Decline as Delete
                showDeleteConfirmation(notif)
            },
            onDeleteClick = { notif ->
                // Show confirmation modal for single delete
                showDeleteConfirmation(notif)
            }
        )
        recyclerView.adapter = adapter


        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }


        btnMarkAllRead.setOnClickListener {
            if (notificationList.isNotEmpty()) {
                dbHelper.markAllNotificationsRead(currentUser) { success ->
                    if (success) {
                        Toast.makeText(context, "All marked as read", Toast.LENGTH_SHORT).show()
                        loadNotifications()
                    }
                }
            }
        }


        btnClearAll.setOnClickListener {
            if (notificationList.isNotEmpty()) {
                showClearAllConfirmation()
            } else {
                Toast.makeText(context, "No notifications to clear", Toast.LENGTH_SHORT).show()
            }
        }


        loadNotifications()
        return view
    }


    // --- POPUP: Delete Single Notification ---
    private fun showDeleteConfirmation(notif: NotificationItem) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)


        tvTitle.text = "DELETE NOTIFICATION?"
        tvMessage.text = "Are you sure you want to remove this notification?"


        btnYes.setOnClickListener {
            dbHelper.deleteNotification(notif.id) {
                loadNotifications()
                Toast.makeText(context, "Notification removed", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        btnNo.setOnClickListener { dialog.dismiss() }


        dialog.show()
    }


    // --- POPUP: Clear All Notifications ---
    private fun showClearAllConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)


        tvTitle.text = "CLEAR ALL?"
        tvMessage.text = "This will delete all your notifications permanently."


        btnYes.setOnClickListener {
            dbHelper.deleteAllNotifications(currentUser) { success ->
                if (success) {
                    Toast.makeText(context, "Notifications cleared", Toast.LENGTH_SHORT).show()
                    loadNotifications()
                }
            }
            dialog.dismiss()
        }
        btnNo.setOnClickListener { dialog.dismiss() }


        dialog.show()
    }


    private fun loadNotifications() {
        dbHelper.getUserNotifications(currentUser) { notifications ->
            notificationList.clear()
            notificationList.addAll(notifications.sortedByDescending { it.id })
            adapter.updateList(notificationList)


            if (notificationList.isEmpty()) {
                layoutEmptyNotifs.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                layoutEmptyNotifs.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }


    private fun acceptInvite(notif: NotificationItem) {
        val groupId = notif.relatedId
        if (groupId != -1) {
            dbHelper.isUserInGroup(groupId, currentUser) { exists ->
                if (exists) {
                    Toast.makeText(context, "Already in group", Toast.LENGTH_SHORT).show()
                    // Silently delete or show separate confirmation? Usually silent clean up is fine here.
                    dbHelper.deleteNotification(notif.id) { loadNotifications() }
                } else {
                    dbHelper.addMemberToGroup(groupId, currentUser) { success ->
                        if (success) {
                            Toast.makeText(context, "Joined Group: ${notif.groupName}", Toast.LENGTH_SHORT).show()
                            // Automatically delete after accept
                            dbHelper.deleteNotification(notif.id) { loadNotifications() }
                        } else {
                            Toast.makeText(context, "Failed to join", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}



