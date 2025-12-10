package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerGroups: RecyclerView
    private lateinit var tvNoGroups: TextView
    private lateinit var currentUser: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        recyclerGroups = view.findViewById(R.id.recyclerGroups)
        tvNoGroups = view.findViewById(R.id.tvNoGroups)
        recyclerGroups.layoutManager = LinearLayoutManager(context)

        // NAVIGATE TO CREATE FRAGMENT
        view.findViewById<Button>(R.id.btnCreateShared).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CreateGroupFragment())
                .addToBackStack(null)
                .commit()
        }

        // NAVIGATE TO JOIN FRAGMENT
        view.findViewById<Button>(R.id.btnJoinShared).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, JoinGroupFragment())
                .addToBackStack(null)
                .commit()
        }

        loadGroups()
        return view
    }

    private fun loadGroups() {
        // --- UPDATED: Async Call with Callback ---
        dbHelper.getUserGroups(currentUser) { groups ->
            if (groups.isEmpty()) {
                recyclerGroups.visibility = View.GONE
                tvNoGroups.visibility = View.VISIBLE
            } else {
                recyclerGroups.visibility = View.VISIBLE
                tvNoGroups.visibility = View.GONE

                recyclerGroups.adapter = GroupAdapter(groups) { selectedGroup ->
                    // Navigate to Group Details
                    val fragment = GroupDetailsFragment()
                    val bundle = Bundle()
                    bundle.putInt("GROUP_ID", selectedGroup.id)
                    bundle.putString("GROUP_NAME", selectedGroup.name)
                    bundle.putString("GROUP_CODE", selectedGroup.code)
                    fragment.arguments = bundle

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    class GroupAdapter(private val groups: ArrayList<GroupInfo>, private val onClick: (GroupInfo) -> Unit) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvGroupName)
            val code: TextView = v.findViewById(R.id.tvGroupCode)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = groups[position]
            holder.name.text = item.name
            holder.code.text = "Code: ${item.code}"
            holder.itemView.setOnClickListener { onClick(item) }
        }
        override fun getItemCount() = groups.size
    }
}