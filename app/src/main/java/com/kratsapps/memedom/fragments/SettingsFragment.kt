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

        maleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.maleFilter)
        femaleFilter = rootView.findViewById<AppCompatRadioButton>(R.id.femaleFilter)
        otherFilter = rootView.findViewById<AppCompatRadioButton>(R.id.otherFilter)

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

        val min = DatabaseManager(settingContext).retrievePrefsInt("minAge", 16)
        val max = DatabaseManager(settingContext).retrievePrefsInt("maxAge", 65)

        minText.setText("$min")
        maxText.setText("$max")

        ageSeekbar.setMinThumbValue(min)
        ageSeekbar.setMaxThumbValue(max)

        ageSeekbar.seekBarChangeListener = object : SeekBarChangeListener {
            override fun onStartedSeeking() {}
            override fun onStoppedSeeking() {

                val minValue = ageSeekbar.getMinThumbValue()

                Log.d(
                    "Filtering",
                    "min ${ageSeekbar.getMinThumbValue()}, max ${ageSeekbar.getMaxThumbValue()}"
                )

                if(minValue >= 16) {
                    DatabaseManager(settingContext).saveToPrefsInt(
                        "minAge",
                        ageSeekbar.getMinThumbValue()
                    )
                }
                DatabaseManager(settingContext).saveToPrefsInt(
                    "maxAge",
                    ageSeekbar.getMaxThumbValue()
                )
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

    private fun activateFemale() {
        femaleFilter.isChecked = true
        otherFilter.isChecked = false
        maleFilter.isChecked = false
        femaleFilter.setTextColor(Color.WHITE)
        maleFilter.setTextColor(Color.parseColor("#ff00ddff"))
        otherFilter.setTextColor(Color.parseColor("#ff00ddff"))

        mainUser?.gender = "Female"
        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser", {})
    }

    private fun activateMale() {
        maleFilter.isChecked = true
        femaleFilter.isChecked = false
        otherFilter.isChecked = false
        maleFilter.setTextColor(Color.WHITE)
        femaleFilter.setTextColor(Color.parseColor("#ff00ddff"))
        otherFilter.setTextColor(Color.parseColor("#ff00ddff"))

        mainUser?.gender = "Male"
        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser", {})
    }

    private fun activateOther() {
        otherFilter.isChecked = true
        maleFilter.isChecked = false
        femaleFilter.isChecked = false
        otherFilter.setTextColor(Color.WHITE)
        femaleFilter.setTextColor(Color.parseColor("#ff00ddff"))
        maleFilter.setTextColor(Color.parseColor("#ff00ddff"))

        mainUser?.gender = "Other"
        DatabaseManager(this.context!!).convertUserObject(mainUser!!, "MainUser", {})
    }

    fun getOpenFacebookIntent(){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/MemeDom420"))
        startActivity(browserIntent)
    }
}