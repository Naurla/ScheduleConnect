package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class ScheduleDetailFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var layoutButtons: LinearLayout
    private lateinit var layoutChangeMind: LinearLayout
    private lateinit var btnCurrentStatus: Button
    private lateinit var currentUser: String
    private var schId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        schId = arguments?.getInt("SCH_ID") ?: -1
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

        layoutButtons = view.findViewById(R.id.layoutRSVPButtons)
        layoutChangeMind = view.findViewById(R.id.layoutChangeMind)
        btnCurrentStatus = view.findViewById(R.id.btnCurrentStatus)

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // 1. Load Status
        refreshStatusUI()

        // 2. RSVP Actions
        view.findViewById<Button>(R.id.btnAttend).setOnClickListener { setRSVP(1) }
        view.findViewById<Button>(R.id.btnUnsure).setOnClickListener { setRSVP(2) }
        view.findViewById<Button>(R.id.btnNotAttend).setOnClickListener { setRSVP(3) }

        // 3. Change Mind Logic
        view.findViewById<TextView>(R.id.tvChangeMind).setOnClickListener {
            layoutChangeMind.visibility = View.GONE
            layoutButtons.visibility = View.VISIBLE
        }

        // 4. View Attendees Logic
        view.findViewById<Button>(R.id.btnViewAttendees).setOnClickListener {
            val fragment = AttendeeListFragment()
            val bundle = Bundle()
            bundle.putInt("SCH_ID", schId)
            bundle.putString("SCH_TITLE", title)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun setRSVP(status: Int) {
        dbHelper.updateRSVP(schId, currentUser, status)
        refreshStatusUI()
    }

    private fun refreshStatusUI() {
        val status = dbHelper.getUserRSVPStatus(schId, currentUser)

        if (status == 0) {
            layoutButtons.visibility = View.VISIBLE
            layoutChangeMind.visibility = View.GONE
        } else {
            layoutButtons.visibility = View.GONE
            layoutChangeMind.visibility = View.VISIBLE

            when (status) {
                1 -> btnCurrentStatus.text = "I WILL ATTEND"
                2 -> btnCurrentStatus.text = "UNSURE"
                3 -> btnCurrentStatus.text = "I WILL NOT ATTEND"
            }
        }
    }
}