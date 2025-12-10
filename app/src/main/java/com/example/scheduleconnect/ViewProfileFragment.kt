package com.example.scheduleconnect

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide // Import Glide

class ViewProfileFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvDOB: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var btnEdit: Button
    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_view_profile, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // Init Views
        tvName = view.findViewById(R.id.tvViewName)
        tvUsername = view.findViewById(R.id.tvViewUsername)
        tvEmail = view.findViewById(R.id.tvViewEmail)
        tvPhone = view.findViewById(R.id.tvViewPhone)
        tvGender = view.findViewById(R.id.tvViewGender)
        tvDOB = view.findViewById(R.id.tvViewDOB)
        ivProfile = view.findViewById(R.id.ivViewProfileImage)
        btnEdit = view.findViewById(R.id.btnGoToEdit)
        btnBack = view.findViewById(R.id.btnBackViewProfile)

        loadData()

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnEdit.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning from Edit
    }

    private fun loadData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "default_user") ?: "default_user"

        // --- ASYNC FETCH ---
        dbHelper.getUserDetails(currentUser) { user ->
            if (user != null) {
                val fullName = "${user.firstName} ${user.middleName} ${user.lastName}".trim()
                tvName.text = if (fullName.isNotEmpty()) fullName else currentUser
                tvUsername.text = "@${user.username}"
                tvEmail.text = user.email.ifEmpty { "No Email" }
                tvPhone.text = user.phone.ifEmpty { "No Phone" }
                tvGender.text = user.gender.ifEmpty { "Not Specified" }
                tvDOB.text = user.dob.ifEmpty { "Not Set" }

                // Load Profile Picture with Glide
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop() // Makes image round
                        .into(ivProfile)

                    ivProfile.imageTintList = null
                } else {
                    // Default State
                    ivProfile.setImageResource(R.drawable.ic_person)
                    ivProfile.setColorFilter(Color.parseColor("#999999"))
                }
            }
        }
    }
}