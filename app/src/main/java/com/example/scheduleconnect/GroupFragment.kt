package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

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

        view.findViewById<Button>(R.id.btnCreateShared).setOnClickListener { showCreateDialog() }
        view.findViewById<Button>(R.id.btnJoinShared).setOnClickListener { showJoinDialog() }

        loadGroups()
        return view
    }

    private fun loadGroups() {
        val groups = dbHelper.getUserGroups(currentUser)
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

    private fun showCreateDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Create Group")
        val input = EditText(requireContext())
        input.hint = "Enter Group Name"
        builder.setView(input)

        builder.setPositiveButton("Create") { _, _ ->
            val groupName = input.text.toString()
            if (groupName.isNotEmpty()) {
                val code = UUID.randomUUID().toString().substring(0, 6).uppercase()
                val success = dbHelper.createGroup(groupName, code, currentUser)
                if (success) {
                    loadGroups()
                    Toast.makeText(context, "Group Created: $code", Toast.LENGTH_LONG).show()
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showJoinDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Join Group")
        val input = EditText(requireContext())
        input.hint = "Enter 6-Character Code"
        builder.setView(input)

        builder.setPositiveButton("Join") { _, _ ->
            val code = input.text.toString().uppercase().trim()
            if (code.isNotEmpty()) {
                val success = dbHelper.joinGroup(currentUser, code)
                if (success) {
                    loadGroups()
                    Toast.makeText(context, "Joined!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid Code or Joined", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
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