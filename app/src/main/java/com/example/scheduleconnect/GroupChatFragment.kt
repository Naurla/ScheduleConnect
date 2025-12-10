package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupChatFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = ArrayList<ChatMessage>()

    private var groupId: Int = -1
    private var groupName: String = ""
    private lateinit var currentUser: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_group_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        groupId = arguments?.getInt("GROUP_ID") ?: -1
        groupName = arguments?.getString("GROUP_NAME") ?: "Group Chat"

        // Initialize Views
        recyclerView = view.findViewById(R.id.recyclerChat)
        etMessage = view.findViewById(R.id.etChatMessage)
        btnSend = view.findViewById(R.id.btnSendMessage)
        btnBack = view.findViewById(R.id.btnBackChat)
        tvTitle = view.findViewById(R.id.tvChatTitle)

        tvTitle.text = groupName

        // Setup RecyclerView
        chatAdapter = ChatAdapter(requireContext(), messageList, currentUser)
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true // Start list from bottom
        }
        recyclerView.adapter = chatAdapter

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        startListeningForMessages()
    }

    private fun sendMessage(message: String) {
        dbHelper.sendGroupMessage(groupId, currentUser, message)
        etMessage.text.clear()
        // No need to manually reload; the SnapshotListener in DatabaseHelper will auto-update
    }

    private fun startListeningForMessages() {
        // Updated to use the callback
        dbHelper.getGroupMessages(groupId) { newMessages ->
            messageList.clear()
            messageList.addAll(newMessages)
            chatAdapter.notifyDataSetChanged()
            if (messageList.isNotEmpty()) {
                recyclerView.scrollToPosition(messageList.size - 1)
            }
        }
    }
}