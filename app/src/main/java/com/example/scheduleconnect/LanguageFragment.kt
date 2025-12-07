package com.example.scheduleconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class LanguageFragment : Fragment() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var rbEnglish: RadioButton
    private lateinit var rbFilipino: RadioButton
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_language, container, false)

        btnBack = view.findViewById(R.id.btnBackLanguage)
        radioGroup = view.findViewById(R.id.radioGroupLanguage)
        rbEnglish = view.findViewById(R.id.rbEnglish)
        rbFilipino = view.findViewById(R.id.rbFilipino)
        btnSave = view.findViewById(R.id.btnSaveLanguage)

        // Load Saved Preference
        val sharedPref = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val currentLang = sharedPref.getString("language", "English")

        if (currentLang == "Filipino") {
            rbFilipino.isChecked = true
        } else {
            rbEnglish.isChecked = true
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val selectedLanguage = if (selectedId == R.id.rbFilipino) "Filipino" else "English"

            // Save to Preferences
            val editor = sharedPref.edit()
            editor.putString("language", selectedLanguage)
            editor.apply()

            Toast.makeText(requireContext(), "Language set to $selectedLanguage", Toast.LENGTH_SHORT).show()

            // Go back
            parentFragmentManager.popBackStack()
        }

        return view
    }
}