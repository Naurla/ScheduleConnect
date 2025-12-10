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
    private lateinit var recyclerChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvGroupName: TextView
    private lateinit var btnSettings: ImageView

    private lateinit var chatAdapter: ChatAdapter
    private var chatList = ArrayList<ChatMessage>()

    private var groupId: Int = -1
    private var groupName: String = ""
    private var currentUser: String = ""

    private var currentNickname: String = ""
    private var currentImageBase64: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_chat, container, false)
        dbHelper = DatabaseHelper(requireContext())

        groupId = arguments?.getInt("GROUP_ID") ?: -1
        groupName = arguments?.getString("GROUP_NAME") ?: "Group Chat"

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        // Init Views
        recyclerChat = view.findViewById(R.id.recyclerChatMessages)
        etMessage = view.findViewById(R.id.etChatMessage)
        btnSend = view.findViewById(R.id.btnSendChat)
        btnBack = view.findViewById(R.id.btnBackChat)
        tvGroupName = view.findViewById(R.id.tvChatGroupName)
        btnSettings = view.findViewById(R.id.btnChatSettings)

        tvGroupName.text = groupName

        // --- FIX IS HERE: Added 'requireContext()' as the first argument ---
        chatAdapter = ChatAdapter(requireContext(), chatList, currentUser)

        recyclerChat.layoutManager = LinearLayoutManager(context)
        recyclerChat.adapter = chatAdapter

        loadMessages()

        checkAdminStatus()

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSend.setOnClickListener { sendMessage() }

        btnSettings.setOnClickListener {
            openGroupSettings()
        }

        return view
    }

    private fun checkAdminStatus() {
        dbHelper.getGroupCreator(groupId) { creator ->
            if (currentUser == creator) {
                btnSettings.visibility = View.VISIBLE
                fetchGroupDetailsForSettings()
            } else {
                btnSettings.visibility = View.GONE
            }
        }
    }

    private fun fetchGroupDetailsForSettings() {
        dbHelper.getGroupDetails(groupId) { group ->
            if (group != null) {
                currentNickname = group.nickname
                currentImageBase64 = group.imageUrl
                if (currentNickname.isNotEmpty()) {
                    tvGroupName.text = currentNickname
                }
            }
        }
    }

    private fun openGroupSettings() {
        val settingsFragment = GroupSettingsFragment()
        val bundle = Bundle()
        bundle.putInt("GROUP_ID", groupId)
        bundle.putString("CURRENT_NICKNAME", currentNickname)
        bundle.putString("CURRENT_IMAGE", currentImageBase64)
        settingsFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun sendMessage() {
        val msg = etMessage.text.toString().trim()
        if (msg.isNotEmpty()) {
            dbHelper.sendGroupMessage(groupId, groupName, currentUser, msg)
            etMessage.text.clear()
        }
    }

    private fun loadMessages() {
        dbHelper.getGroupMessages(groupId) { messages ->
            chatList.clear()
            chatList.addAll(messages)
            chatAdapter.notifyDataSetChanged()
            if (chatList.isNotEmpty()) {
                recyclerChat.scrollToPosition(chatList.size - 1)
            }
        }
    }
}