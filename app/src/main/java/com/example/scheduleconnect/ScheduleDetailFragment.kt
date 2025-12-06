package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ScheduleDetailFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_details, container, false)
        val dbHelper = DatabaseHelper(requireContext())

        val schId = arguments?.getInt("SCH_ID") ?: -1
        val title = arguments?.getString("SCH_TITLE")
        val date = arguments?.getString("SCH_DATE")
        val loc = arguments?.getString("SCH_LOC")
        val desc = arguments?.getString("SCH_DESC")
        val creator = arguments?.getString("SCH_CREATOR")

        view.findViewById<TextView>(R.id.tvDetailTitle).text = title
        view.findViewById<TextView>(R.id.tvDetailDate).text = date
        view.findViewById<TextView>(R.id.tvDetailLoc).text = loc
        view.findViewById<TextView>(R.id.tvDetailDesc).text = desc
        view.findViewById<TextView>(R.id.tvDetailCreator).text = "Schedule by: $creator"

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        view.findViewById<Button>(R.id.btnAttend).setOnClickListener {
            dbHelper.updateRSVP(schId, currentUser, 1)
            Toast.makeText(context, "You are Attending", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.btnUnsure).setOnClickListener {
            dbHelper.updateRSVP(schId, currentUser, 2)
            Toast.makeText(context, "Marked as Unsure", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.btnNotAttend).setOnClickListener {
            dbHelper.updateRSVP(schId, currentUser, 3)
            Toast.makeText(context, "You are NOT Attending", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}