package com.kratsapps.memedom.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.fragment.app.Fragment
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.innovattic.rangeseekbar.RangeSeekBar
import com.innovattic.rangeseekbar.RangeSeekBar.SeekBarChangeListener
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_main.*


class SettingsFragment : Fragment() {

    lateinit var settingContext: Context
    lateinit var rootView: View
    lateinit var maleFilter: AppCompatRadioButton
    lateinit var femaleFilter: AppCompatRadioButton
    lateinit var otherFilter: AppCompatRadioButton

    lateinit var lookingMaleFilter: AppCompatRadioButton
    lateinit var lookingFemaleFilter: AppCompatRadioButton
    lateinit var lookingOtherFilter: AppCompatRadioButton
    var mainUser: MemeDomUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingContext = context
        Log.d("OnCreateView", "Called Attached")
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

        Log.d("MainUserFilters", "Gender ${mainUser?.gender} LookingFor ${mainUser?.lookingFor} min ${mainUser?.minAge} max ${mainUser?.maxAge}")

        maleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.genderMale)
        femaleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.genderFemale)
        otherFilter = rootView.findViewById<AppCompatRadioButton>(R.id.genderOther)

        lookingMaleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.lookingMaleFilter)
        lookingFemaleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.lookingFemaleFilter)
        lookingOtherFilter = rootView.findViewById<AppCompatRadioButton>(R.id.lookingOtherFilter)

        val signoutBtn = rootView.findViewById<Button>(R.id.signoutBtn)
        val contactBtn = rootView.findViewById<Button>(R.id.contactBtn)

        contactBtn.setOnClickListener {
            getOpenFacebookIntent()
        }

        signoutBtn.setOnClickListener {
            Toast.makeText(settingContext, "Successfully Signed Out", Toast.LENGTH_SHORT).show()
            LoginManager.getInstance().logOut()
            FirebaseAuth.getInstance().signOut()
            returnToLoggedOutState()
        }

        if (mainUser != null) {

            val mainUserGender = mainUser!!.gender
            val mainLookingFor = mainUser!!.lookingFor

            if (mainUserGender == "Male") {
                activateFilter(maleFilter, "Male", null, listOf(femaleFilter, otherFilter))
            } else if (mainUserGender == "Female") {
                activateFilter(femaleFilter, "Female", null, listOf(otherFilter, maleFilter))
            } else {
                activateFilter(otherFilter, "Other", null, listOf(maleFilter, femaleFilter))
            }

            if (mainLookingFor == "Male") {
                activateFilter(lookingMaleFilter, null, "Male", listOf(lookingFemaleFilter, lookingOtherFilter))
            } else if (mainLookingFor == "Female") {
                activateFilter(lookingFemaleFilter, null, "Male", listOf(lookingMaleFilter, lookingOtherFilter))
            } else {
                activateFilter(lookingOtherFilter, null, "Other", listOf(lookingMaleFilter, lookingFemaleFilter))
            }

            maleFilter.setOnClickListener {
                activateFilter(maleFilter, "Male", null, listOf(femaleFilter, otherFilter))
            }

            femaleFilter.setOnClickListener {
                activateFilter(femaleFilter, "Female", null, listOf(otherFilter, maleFilter))
            }

            otherFilter.setOnClickListener {
                activateFilter(otherFilter, "Other", null, listOf(maleFilter, femaleFilter))
            }

            lookingMaleFilter.setOnClickListener {
                activateFilter(lookingMaleFilter, null, "Male", listOf(lookingFemaleFilter, lookingOtherFilter))
            }

            lookingFemaleFilter.setOnClickListener {
                activateFilter(lookingFemaleFilter, null, "Male", listOf(lookingMaleFilter, lookingOtherFilter))
            }

            lookingOtherFilter.setOnClickListener {
                activateFilter(lookingOtherFilter, null, "Other", listOf(lookingMaleFilter, lookingFemaleFilter))
            }
        }

        setupSeekBar()
    }

    private fun sendEmail() {
        val receipient: String = "krats.apps@gmail.com"
        val subject: String = "Memedom Contact"
        val message: String = ""

        val mIntent = Intent(Intent.ACTION_SEND)
        mIntent.data = Uri.parse("mailto:")
        mIntent.type = "text/plain"
        mIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(receipient))
        mIntent.putExtra(Intent.EXTRA_SUBJECT, subject)

        try {
            startActivity(Intent.createChooser(mIntent, "Select Email Client"))
        } catch (e: Exception) {
            Toast.makeText(this.settingContext, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun returnToLoggedOutState() {
        val parentActivity = this.activity as MainActivity
        parentActivity.navigationBottom.visibility = View.GONE
        val homeFragment = HomeFragment()
        parentActivity.makeCurrentFragment(homeFragment)
    }

    private fun setupSeekBar() {

        val ageSeekbar = rootView.findViewById<RangeSeekBar>(R.id.seekBar)
        val minText = rootView.findViewById<TextView>(R.id.minText)
        val maxText = rootView.findViewById<TextView>(R.id.maxText)

        val min = if (mainUser != null) mainUser?.minAge else 16
        val max = if (mainUser != null) mainUser?.maxAge else 65

        minText.setText("$min")
        maxText.setText("$max")

        ageSeekbar.setMinThumbValue(min!!)
        ageSeekbar.setMaxThumbValue(max!!)

        ageSeekbar.seekBarChangeListener = object : SeekBarChangeListener {
            override fun onStartedSeeking() {}
            override fun onStoppedSeeking() {

                val minValue = ageSeekbar.getMinThumbValue()

                Log.d(
                    "Filtering",
                    "min ${ageSeekbar.getMinThumbValue()}, max ${ageSeekbar.getMaxThumbValue()}"
                )

                if(minValue >= 16) {
                    mainUser?.minAge = ageSeekbar.getMinThumbValue()
                }
                mainUser?.maxAge = ageSeekbar.getMaxThumbValue()
                DatabaseManager(settingContext).convertUserObject(mainUser!!, "MainUser", {})
            }

            override fun onValueChanged(minThumbValue: Int, maxThumbValue: Int) {

                val minValue = minThumbValue
                val maxValue = maxThumbValue

                if (minThumbValue >= 16) {
                    minText.setText(minValue.toString())
                }
                maxText.setText(maxValue.toString())
            }
        }
    }

    private fun activateFilter(active: AppCompatRadioButton, gender: String?, lookingFor: String?, deactives: List<AppCompatRadioButton>) {
        for (segment in deactives) {
            segment.isChecked = false
        }

        active.isChecked = true

        if (gender != null) {
            mainUser?.gender = gender
        }

        if (lookingFor != null) {
            mainUser?.lookingFor = lookingFor
        }

        DatabaseManager(settingContext).convertUserObject(mainUser!!, "MainUser", {})
    }

    fun getOpenFacebookIntent(){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/MemeDom420"))
        startActivity(browserIntent)
    }
}