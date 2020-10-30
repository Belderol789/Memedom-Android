package com.kratsapps.memedom.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.widget.AppCompatRadioButton
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_signup.*


class SettingsFragment : Fragment() {

    lateinit var rootView: View
    lateinit var maleFilter: AppCompatRadioButton
    lateinit var femaleFilter: AppCompatRadioButton
    lateinit var otherFilter: AppCompatRadioButton
    var mainUser: MemeDomUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        setupUI()
        return rootView
    }

    private fun setupUI() {
        mainUser = DatabaseManager(this.context!!).retrieveSavedUser()

        maleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.maleFilter)
        femaleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.femaleFilter)
        otherFilter = rootView.findViewById<AppCompatRadioButton>(R.id.otherFilter)

        if(mainUser?.gender.equals("Female")) {
            activateFemale()
        } else if (mainUser?.gender.equals("Male")) {
            activateMale()
        } else {
            activateOther()
        }

        if (mainUser != null) {
            maleFilter.setOnClickListener {
                activateMale()
            }

            femaleFilter.setOnClickListener {
                activateFemale()
            }

            otherFilter.setOnClickListener {
                activateOther()
            }
        }
    }

    private fun activateFemale() {
        femaleFilter.isChecked = true
        otherFilter.isChecked = false
        maleFilter.isChecked = false
        femaleFilter.setTextColor(Color.WHITE)
        maleFilter.setTextColor(Color.parseColor("#ff00ddff"))
        otherFilter.setTextColor(Color.parseColor("#ff00ddff"))

        mainUser?.gender = "Female"
        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser")
    }

    private fun activateMale() {
        maleFilter.isChecked = true
        femaleFilter.isChecked = false
        otherFilter.isChecked = false
        maleFilter.setTextColor(Color.WHITE)
        femaleFilter.setTextColor(Color.parseColor("#ff00ddff"))
        otherFilter.setTextColor(Color.parseColor("#ff00ddff"))

        mainUser?.gender = "Male"
        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser")
    }

    private fun activateOther() {
        otherFilter.isChecked = true
        maleFilter.isChecked = false
        femaleFilter.isChecked = false
        otherFilter.setTextColor(Color.WHITE)
        femaleFilter.setTextColor(Color.parseColor("#ff00ddff"))
        maleFilter.setTextColor(Color.parseColor("#ff00ddff"))

        mainUser?.gender = "Other"
        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser")
    }



}