package com.example.scheduleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupDetailsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private var groupId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group_details, container, false)
        dbHelper = DatabaseHelper(requireContext())

        groupId = arguments?.getInt("GROUP_ID") ?: -1
        val groupName = arguments?.getString("GROUP_NAME") ?: ""
        val groupCode = arguments?.getString("GROUP_CODE") ?: ""

        view.findViewById<TextView>(R.id.tvGroupDetailName).text = groupName
        view.findViewById<TextView>(R.id.tvGroupDetailCode).text = "SHARED SCHEDULE CODE: $groupCode"

        recycler = view.findViewById(R.id.recyclerGroupSchedules)
        tvEmpty = view.findViewById(R.id.tvEmptyGroupSchedule)
        recycler.layoutManager = LinearLayoutManager(context)

        view.findViewById<Button>(R.id.btnAddSharedSchedule).setOnClickListener {
            val fragment = AddSharedScheduleFragment()
            val bundle = Bundle()
            bundle.putInt("GROUP_ID", groupId)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<ImageView>(R.id.btnBackGroup).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadSchedules()
        return view
    }

    private fun loadSchedules() {
        val list = dbHelper.getGroupSchedules(groupId)

        if (list.isEmpty()) {
            recycler.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val adapter = ScheduleAdapter(list)
            recycler.adapter = adapter

            adapter.setOnItemClickListener { schedule ->
                val fragment = ScheduleDetailFragment()
                val bundle = Bundle()
                bundle.putInt("SCH_ID", schedule.id)
                bundle.putString("SCH_TITLE", schedule.title)
                bundle.putString("SCH_DATE", schedule.date)
                bundle.putString("SCH_LOC", schedule.location)
                bundle.putString("SCH_DESC", schedule.description)
                bundle.putString("SCH_CREATOR", schedule.creator)
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}