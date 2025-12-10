package com.example.scheduleconnect

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

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

        // Init Views (Initialization logic remains correct)
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
        loadData()
    }

    private fun loadData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("USERNAME", "default_user") ?: "default_user"

        dbHelper.getUserDetails(currentUser) { user ->
            if (user != null) {
                // Personal Info Display
                val fullName = "${user.firstName} ${user.middleName} ${user.lastName}".trim()
                tvName.text = if (fullName.isNotEmpty()) fullName else currentUser
                tvUsername.text = "@${user.username}"
                tvEmail.text = user.email.ifEmpty { "No Email" }
                tvPhone.text = user.phone.ifEmpty { "No Phone" }
                tvGender.text = user.gender.ifEmpty { "Not Specified" }
                tvDOB.text = user.dob.ifEmpty { "Not Set" }

                // Image Loading
                loadProfileImage(user.profileImageUrl)
            }
        }
    }

    private fun loadProfileImage(base64Image: String) {
        if (base64Image.isNotEmpty()) {
            try {
                val decodedByte = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)

                // CRITICAL FIX: Use post to ensure the image is set after the view hierarchy is stable.
                // This prevents the flash-to-black/GC issue.
                ivProfile.post {
                    ivProfile.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivProfile.setImageBitmap(bitmap)
                    ivProfile.imageTintList = null
                    ivProfile.setPadding(0, 0, 0, 0)
                }

            } catch (e: Exception) {
                setDefaultProfileImage()
            }
        } else {
            setDefaultProfileImage()
        }
    }

    private fun setDefaultProfileImage() {
        // Default State
        ivProfile.setImageResource(R.drawable.ic_person)
        ivProfile.setColorFilter(Color.parseColor("#999999"))
        ivProfile.scaleType = ImageView.ScaleType.FIT_CENTER
        // Set back to default padding for the icon to fit inside the circular boundary
        val density = requireContext().resources.displayMetrics.density
        val paddingPixel = (20 * density).toInt()
        ivProfile.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel)
    }
}