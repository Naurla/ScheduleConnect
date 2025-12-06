package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class JoinGroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_join_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val etCode = view.findViewById<EditText>(R.id.etJoinCode)
        val btnJoin = view.findViewById<Button>(R.id.btnFinalizeJoin)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackJoin)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnJoin.setOnClickListener {
            val code = etCode.text.toString().uppercase().trim()

            if (code.isEmpty()) {
                etCode.error = "Code required"
                return@setOnClickListener
            }

            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

            val success = dbHelper.joinGroup(currentUser, code)

            if (success) {
                Toast.makeText(context, "Joined Group Successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Go back to Group List
            } else {
                Toast.makeText(context, "Invalid Code or Already Joined", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}