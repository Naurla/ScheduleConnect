package com.example.scheduleconnect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HistoryFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // You can create a simple XML for this: fragment_history.xml
        return inflater.inflate(R.layout.fragment_history, container, false)
    }
}