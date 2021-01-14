package com.kratsapps.memedom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatRadioButton
import com.bumptech.glide.Glide
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.innovattic.rangeseekbar.RangeSeekBar
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.activity_edit.loadingImageView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_signup.*

class EditActivity : AppCompatActivity() {

    var mainUser: MemeDomUser? = null
    var updatedProfilePhotoString: String = ""

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        mainUser = DatabaseManager(this).retrieveSavedUser()
        setupUI()
        setupUIAction()
        setupGenderLookingFor()
    }

    private fun setupUI() {

        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)

        val min = if (mainUser != null) mainUser?.minAge else 16
        val max = if (mainUser != null) mainUser?.maxAge else 65

        ageSeekBar.setMinThumbValue(min!!)
        ageSeekBar.setMaxThumbValue(max!!)

        if (min >= 16) {
            minText.setText(min.toString())
        }
        maxText.setText(max.toString())

        if (mainUser != null) {
            editUsernameText.setText(mainUser!!.name)
            editBioText.setText(mainUser!!.bio)

            updatedProfilePhotoString = mainUser!!.profilePhoto

            Glide.with(this)
                .load(mainUser!!.profilePhoto)
                .circleCrop()
                .into(editProfilePhoto)
        }
    }

    private fun setupUIAction() {

        editProfilePhoto.setOnClickListener {
            prepOpenImageGallery()
        }

        editCameraBtn.setOnClickListener {
            prepOpenImageGallery()
        }

        contactBtn.setOnClickListener {
            getOpenFacebookIntent()
        }

        saveEditsBtn.setOnClickListener {
            saveProfileEdits()
        }

        signoutBtn.setOnClickListener {
            DatabaseManager(this).saveToPrefsBoolean("TutorialKey", false)
            Toast.makeText(this, "Successfully Signed Out", Toast.LENGTH_SHORT).show()
            LoginManager.getInstance().logOut()
            FirebaseAuth.getInstance().signOut()
            DatabaseManager(this).convertUserObject(null,  {})
            returnToLoggedOutState()
        }

        editBackBtn.setOnClickListener {
            userTappedBack()
        }

        ageSeekBar.seekBarChangeListener = object : RangeSeekBar.SeekBarChangeListener {
            override fun onStartedSeeking() {}
            override fun onStoppedSeeking() {

                val minValue = ageSeekBar.getMinThumbValue()

                Log.d(
                    "Filtering",
                    "min ${ageSeekBar.getMinThumbValue()}, max ${ageSeekBar.getMaxThumbValue()}"
                )

                if(minValue >= 16) {
                    mainUser?.minAge = ageSeekBar.getMinThumbValue()
                }
                mainUser?.maxAge = ageSeekBar.getMaxThumbValue()
                DatabaseManager(this@EditActivity).convertUserObject(mainUser,  {})
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

    fun userTappedBack() {
        setResult(Activity.RESULT_OK, intent)
        onBackPressed()
    }

    fun saveProfileEdits() {
        if (mainUser != null) {
            val savedBio = mainUser!!.bio
            val savedProfilePhotoURL = mainUser!!.profilePhoto

            val currentBio = editBioText.text.toString()
            val currentProfilePhoto = updatedProfilePhotoString

            if (currentBio != savedBio || currentProfilePhoto != savedProfilePhotoURL) {
                editLoadingView.visibility = View.VISIBLE
                mainUser?.bio = editBioText.text.toString()
                mainUser?.profilePhoto = updatedProfilePhotoString
                DatabaseManager(this).convertUserObject(mainUser!!,  {})

                val hashMap: HashMap<String, Any> = hashMapOf(
                    "bio" to currentBio,
                    "profilePhoto" to currentProfilePhoto
                )
                FirestoreHandler().updateDatabaseObject("User", mainUser!!.uid, hashMap)
                Toast.makeText(baseContext, "Edits Saved!", Toast.LENGTH_SHORT).show()
                editLoadingView.visibility = View.INVISIBLE
            }
        }
    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    private fun setupGenderLookingFor() {
        if (mainUser != null) {

            val mainUserGender = mainUser!!.gender
            val mainLookingFor = mainUser!!.lookingFor

            Log.d("UserGender", "Gender $mainUserGender LookingFor $mainLookingFor")

            when (mainUserGender) {
                "Male" -> {
                    activateFilter(genderMale, "Male", null, listOf(genderFemale, genderOther))
                }
                "Female" -> {
                    activateFilter(genderFemale, "Female", null, listOf(genderOther, genderMale))
                }
                else -> {
                    activateFilter(genderOther, "Other", null, listOf(genderMale, genderFemale))
                }
            }

            when (mainLookingFor) {
                "Male" -> {
                    activateFilter(lookingMaleFilter, null, "Male", listOf(lookingFemaleFilter, lookingOtherFilter))
                }
                "Female" -> {
                    activateFilter(lookingFemaleFilter, null, "Female", listOf(lookingMaleFilter, lookingOtherFilter))
                }
                else -> {
                    activateFilter(lookingOtherFilter, null, "Other", listOf(lookingMaleFilter, lookingFemaleFilter))
                }
            }

            genderMale.setOnClickListener {
                activateFilter(genderMale, "Male", null, listOf(genderFemale, genderOther))
            }

            genderFemale.setOnClickListener {
                activateFilter(genderFemale, "Female", null, listOf(genderOther, genderMale))
            }

            genderOther.setOnClickListener {
                activateFilter(genderOther, "Other", null, listOf(genderMale, genderFemale))
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

        DatabaseManager(this).convertUserObject(mainUser!!, {})
    }

    private fun returnToLoggedOutState() {
        val intent: Intent = Intent(this, InitialActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun getOpenFacebookIntent(){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/MemeDom420"))
        startActivity(browserIntent)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageData: Uri? = data?.data
        if (imageData != null && mainUser != null && requestCode == IMAGE_GALLERY_REQUEST_CODE) {
            editLoadingView.visibility = View.VISIBLE
            FireStorageHandler().uploadPhotoData(mainUser!!.uid, imageData, this, {
                updatedProfilePhotoString = it
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(editProfilePhoto)
                editLoadingView.visibility = View.INVISIBLE
            })
        }
    }

}