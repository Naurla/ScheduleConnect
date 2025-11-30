package com.example.scheduleconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class AddScheduleFragment : Fragment() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_schedule, container, false)
        dbHelper = DatabaseHelper(requireContext())

        val etName = view.findViewById<EditText>(R.id.etSchName)
        val etDate = view.findViewById<EditText>(R.id.etSchDate)
        val etLoc = view.findViewById<EditText>(R.id.etSchLocation)
        val etDesc = view.findViewById<EditText>(R.id.etSchDesc)
        val btnAdd = view.findViewById<Button>(R.id.btnAddSchedule)

        btnAdd.setOnClickListener {
            val name = etName.text.toString()
            val date = etDate.text.toString()
            val loc = etLoc.text.toString()
            val desc = etDesc.text.toString()

            if (name.isNotEmpty() && date.isNotEmpty()) {
                // For now, type is always "personal"
                val success = dbHelper.addSchedule(name, date, loc, desc, "personal")
                if (success) {
                    Toast.makeText(context, "Schedule Added!", Toast.LENGTH_SHORT).show()
                    // Clear fields
                    etName.text.clear()
                    etDate.text.clear()
                    etLoc.text.clear()
                    etDesc.text.clear()
                } else {
                    Toast.makeText(context, "Error adding schedule", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Name and Date required", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }
}