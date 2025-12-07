package com.example.scheduleconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Locale

class LanguageFragment : Fragment() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView
    private var selectedLanguageCode: String = "en" // Default

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_language, container, false)

        radioGroup = view.findViewById(R.id.radioGroupLanguage) // Ensure ID matches your XML
        btnSave = view.findViewById(R.id.btnSaveLanguage) // Ensure ID matches your XML
        btnBack = view.findViewById(R.id.btnBackLanguage) // Ensure ID matches your XML

        // Load saved language to check the correct radio button
        val sharedPref = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val currentLang = sharedPref.getString("My_Lang", "en") ?: "en"

        // Set the initial selection based on saved preference
        if (currentLang == "fil") {
            view.findViewById<RadioButton>(R.id.rbFilipino).isChecked = true
        } else {
            view.findViewById<RadioButton>(R.id.rbEnglish).isChecked = true
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            // Determine which language was selected
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == R.id.rbFilipino) {
                selectedLanguageCode = "fil"
            } else {
                selectedLanguageCode = "en"
            }

            // Show Confirmation Modal
            showConfirmationDialog(selectedLanguageCode)
        }

        return view
    }

    private fun showConfirmationDialog(languageCode: String) {
        val builder = AlertDialog.Builder(requireContext())
        // Re-using your generic confirmation dialog layout
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_generic_confirmation, null)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnYes = view.findViewById<Button>(R.id.btnDialogYes)
        val btnNo = view.findViewById<TextView>(R.id.btnDialogNo)

        tvTitle.text = "Change Language?"
        val langName = if (languageCode == "fil") "Filipino" else "English"
        tvMessage.text = "Are you sure you want to change the language to $langName? The app will restart."

        btnNo.setOnClickListener { dialog.dismiss() }

        btnYes.setOnClickListener {
            dialog.dismiss()
            setLocale(languageCode)
        }

        dialog.show()
    }

    private fun setLocale(lang: String) {
        // Use the new helper to save and set
        LocaleHelper.setLocale(requireContext(), lang)

        // Restart App
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}