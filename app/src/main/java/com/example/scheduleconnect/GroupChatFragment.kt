package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager // Required for keyboard adjustment
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

        chatAdapter = ChatAdapter(requireContext(), chatList, currentUser, groupId, dbHelper)

        // Setup LayoutManager with stackFromEnd to keep messages at the bottom
        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = true
        recyclerChat.layoutManager = layoutManager
        recyclerChat.adapter = chatAdapter

        loadMessages()

        // --- FIX: Auto-scroll to bottom when the keyboard opens ---
        recyclerChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                recyclerChat.postDelayed({
                    if (chatList.isNotEmpty()) {
                        recyclerChat.scrollToPosition(chatList.size - 1)
                    }
                }, 100)
            }
        }

        btnSettings.visibility = View.VISIBLE
        fetchGroupDetailsForSettings()

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSend.setOnClickListener { sendMessage() }
        btnSettings.setOnClickListener { openGroupSettings() }

        return view
    }

    override fun onResume() {
        super.onResume()
        // 1. Hide global UI elements
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE
        activity?.findViewById<View>(R.id.llHeader)?.visibility = View.GONE

        // 2. DYNAMIC FIX: Switch to adjustResize so the Group Name stays at the top
        // This stops the whole screen from "panning" (sliding up)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onPause() {
        super.onPause()
        // 3. RESTORE: Revert back to adjustPan for Login/Signup pages
        // to prevent icons from jumping on those screens
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onStop() {
        super.onStop()
        // Show navigation again when leaving
        activity?.findViewById<View>(R.id.bottomNav)?.visibility = View.VISIBLE
        activity?.findViewById<View>(R.id.llHeader)?.visibility = View.VISIBLE
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
            // Messages will auto-reload and scroll via the database listener
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