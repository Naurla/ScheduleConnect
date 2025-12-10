package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class ChangePasswordFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etCurrent: TextInputEditText
    private lateinit var etNew: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var btnUpdate: Button
    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        dbHelper = DatabaseHelper(requireContext())

        etCurrent = view.findViewById(R.id.etCurrentPass)
        etNew = view.findViewById(R.id.etNewPass)
        etConfirm = view.findViewById(R.id.etConfirmPass)
        btnUpdate = view.findViewById(R.id.btnUpdatePass)
        btnBack = view.findViewById(R.id.btnBackChangePass)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUpdate.setOnClickListener {
            val currentPass = etCurrent.text.toString().trim()
            val newPass = etNew.text.toString().trim()
            val confirmPass = etConfirm.text.toString().trim()

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 8) {
                Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get Current User
            val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val username = sharedPref.getString("username", "") ?: ""

            if (username.isNotEmpty()) {
                // UI Feedback: Disable button to prevent double-click
                btnUpdate.isEnabled = false
                btnUpdate.text = "Updating..."

                // 1. Verify Old Password (Async)
                dbHelper.checkUser(username, currentPass) { isValid ->
                    if (isValid) {
                        // 2. Get User Details to get Email (Async)
                        // We need the email because the updatePassword function identifies users by email/phone
                        dbHelper.getUserDetails(username) { userDetails ->
                            if (userDetails != null) {
                                // 3. Update Password (Async)
                                dbHelper.updatePassword(userDetails.email, newPass, true) { success ->
                                    btnUpdate.isEnabled = true
                                    btnUpdate.text = "UPDATE PASSWORD"

                                    if (success) {
                                        Toast.makeText(context, "Password Updated Successfully!", Toast.LENGTH_SHORT).show()
                                        parentFragmentManager.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Failed to update password", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                btnUpdate.isEnabled = true
                                btnUpdate.text = "UPDATE PASSWORD"
                                Toast.makeText(context, "User details not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        btnUpdate.isEnabled = true
                        btnUpdate.text = "UPDATE PASSWORD"
                        Toast.makeText(context, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Session Error. Please relogin.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}