package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JoinGroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etCode: EditText
    private lateinit var btnJoin: Button
    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_join_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        etCode = view.findViewById(R.id.etJoinCode)
        btnJoin = view.findViewById(R.id.btnJoinGroupFinal)
        btnBack = view.findViewById(R.id.btnBackJoin)

        // Optional: Force uppercase input filter
        etCode.filters = arrayOf(InputFilter.AllCaps())

        // --- NEW FIX: Auto-fill code if passed from arguments ---
        arguments?.getString("group_code")?.let { code ->
            if (code != "N/A") {
                etCode.setText(code)
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnJoin.setOnClickListener {
            val code = etCode.text.toString().trim().uppercase()

            if (code.isEmpty()) {
                etCode.error = "Please enter a code"
                return@setOnClickListener
            }

            // Get Current User
            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

            // Disable button to prevent double clicks
            btnJoin.isEnabled = false
            btnJoin.text = "Checking..."

            // 1. Find Group by Code (Async)
            dbHelper.getGroupIdByCode(code) { groupId ->
                if (groupId != -1) {
                    // 2. Check if already a member (Async)
                    dbHelper.isUserInGroup(groupId, currentUser) { isMember ->
                        if (isMember) {
                            Toast.makeText(context, "You are already in this group!", Toast.LENGTH_SHORT).show()
                            btnJoin.isEnabled = true
                            btnJoin.text = "JOIN GROUP"
                        } else {
                            // 3. Add Member (Async)
                            dbHelper.addMemberToGroup(groupId, currentUser) { success ->
                                if (success) {
                                    // 4. Notify Creator (Async)
                                    dbHelper.getGroupCreator(groupId) { creator ->
                                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        // Note: We use the Code as the name here since fetching the name requires another call
                                        dbHelper.addNotification(creator, "New Member Joined", "$currentUser joined using code: $code", date)

                                        Toast.makeText(context, "Successfully Joined Group!", Toast.LENGTH_SHORT).show()
                                        parentFragmentManager.popBackStack()
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to join group", Toast.LENGTH_SHORT).show()
                                    btnJoin.isEnabled = true
                                    btnJoin.text = "JOIN GROUP"
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Invalid Group Code", Toast.LENGTH_SHORT).show()
                    btnJoin.isEnabled = true
                    btnJoin.text = "JOIN GROUP"
                }
            }
        }

        return view
    }
}
