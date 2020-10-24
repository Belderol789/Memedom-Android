package com.kratsapps.memedom

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.*
import kotlinx.android.synthetic.main.activity_signup.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class SignupActivity : AppCompatActivity() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    private var screenWidth: Int = 0

    private lateinit var auth: FirebaseAuth
    lateinit var memeDomuser: MemeDomUser
    lateinit var progressOverlay: View

    private val mAuth: FirebaseAuth? = null
    var userEmail: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()
        userEmail = intent.getBooleanExtra("AUTH_METHOD", true)

        Log.i("Navigation", "Navigated to Signup")
        setupUI()
        setupActionButtons()
        setupTextEdits()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 121 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            imageButtonProfile.isClickable = true
        }
    }

    private fun requestPermissionToPhotos() {
        if(ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                Array(1) { android.Manifest.permission.READ_EXTERNAL_STORAGE },
                121
            )
        } else {
            imageButtonProfile.isClickable = true
        }
    }

    private fun setupUI() {
        progressOverlay = findViewById(R.id.progress_overlay)
        screenWidth = ScreenSize().getScreenWidth()

        Log.d("Screen Size", "ScreenWidth ${screenWidth}")

        layoutAuth.layoutParams.width = screenWidth
        layoutUsername.layoutParams.width = screenWidth
        layoutBirthday.layoutParams.width = screenWidth
        layoutGender.layoutParams.width = screenWidth

        textEditBirthday.setRawInputType(InputType.TYPE_NULL)
        scrollViewSignup.setOnTouchListener(View.OnTouchListener { v, event -> true })

        //For Facebook
        if (!userEmail) {
            memeDomuser = intent.extras?.get("MEMEDOM_USER") as MemeDomUser
            layoutAuth.visibility = View.INVISIBLE
            textEditUsername.setText(memeDomuser.name)
            textEditBirthday.setText(memeDomuser.birthday)
            Glide.with(this)
                .load(memeDomuser.profilePhoto)
                .centerCrop()
                .into(imageButtonProfile)
            requestPermissionToPhotos()
            Timer().schedule(1000) {
                scrollViewSignup.smoothScrollTo(screenWidth, 0)
            }
        } else {
            memeDomuser = MemeDomUser()
        }
    }

    private fun setupActionButtons() {
        // Authentication
        if (userEmail) {
            buttonNextAuth.setOnClickListener{
                checkIfFieldsHaveValues()
            }
        }

        // Username
        buttonNextUsername.setOnClickListener{
            val username = textEditUsername.text.toString()
            if(!username.isEmpty()) {
                memeDomuser.name = username
                scrollViewSignup.smoothScrollTo(screenWidth * 2, 0)
            }
        }
        // Profile Photo
        imageButtonProfile.setOnClickListener{
            prepOpenImageGallery()
        }

        // Gender
        buttonNextGender.setOnClickListener {
            if(!memeDomuser.gender.isEmpty()) {
                scrollViewSignup.smoothScrollTo(screenWidth * 3, 0)
            }
        }

        maleSegment.setOnClickListener{
            Log.d("Segment", "Male segment tapped")
            memeDomuser.gender = "Male"
            updateGenderSegments(0)
        }
        femaleSegment.setOnClickListener{
            Log.d("Segment", "Female segment tapped")
            memeDomuser.gender = "Female"
            updateGenderSegments(1)
        }
        otherSegment.setOnClickListener{
            memeDomuser.gender = "Other"
            Log.d("Segment", "Other segment tapped")
            updateGenderSegments(2)
        }

        textEditBirthday.setText("")
        var cal = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val myFormat = "dd MMMM yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            textEditBirthday.setText(sdf.format(cal.time))
        }

        textEditBirthday.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonNextBirthday.setOnClickListener{
            signupUser()
        }
    }

    private fun navigateToMain() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    private fun checkIfFieldsHaveValues() {
        hideKeyboard()

        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()

        if(email.isEmpty() || password.toString().isEmpty() || password.toString().length < 6) {
            Toast.makeText(baseContext, "Email or Password is invalid", Toast.LENGTH_SHORT).show()
        } else {
            userDidAuthEmail(email, password)
        }
    }

    private fun userDidAuthEmail(email: String, password: String) {
        AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressOverlay.visibility = View.GONE
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        memeDomuser.uid = user.uid
                        memeDomuser.email = email
                        requestPermissionToPhotos()
                        scrollViewSignup.smoothScrollTo(screenWidth, 0)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Authentication", "createUserWithEmail:failure", task.exception)
                    setupAlertDialog(task.exception?.message)
                }
                // ...
            }
    }

    private fun updateGenderSegments(type: Int) {

        if(type == 0) {
            maleSegment.isChecked = true
            femaleSegment.isChecked = false
            otherSegment.isChecked = false
            maleSegment.setTextColor(Color.WHITE)
            femaleSegment.setTextColor(Color.parseColor("#ff00ddff"))
            otherSegment.setTextColor(Color.parseColor("#ff00ddff"))
        } else if (type == 1) {
            femaleSegment.isChecked = true
            otherSegment.isChecked = false
            maleSegment.isChecked = false
            femaleSegment.setTextColor(Color.WHITE)
            maleSegment.setTextColor(Color.parseColor("#ff00ddff"))
            otherSegment.setTextColor(Color.parseColor("#ff00ddff"))
        } else {
            otherSegment.isChecked = true
            maleSegment.isChecked = false
            femaleSegment.isChecked = false
            otherSegment.setTextColor(Color.WHITE)
            femaleSegment.setTextColor(Color.parseColor("#ff00ddff"))
            maleSegment.setTextColor(Color.parseColor("#ff00ddff"))
        }
    }

    private fun setupAlertDialog(message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Signup Error")
        builder.setMessage(message)

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            Toast.makeText(applicationContext, R.string.alert_ok, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val button = findViewById<ImageButton>(R.id.imageButtonProfile)
            button.setColorFilter(Color.parseColor("#00ff0000"))
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {
                imageButtonProfile.drawable.setTint(Color.parseColor("#00ff0000"))
                val imageData = data.data
                Glide.with(this)
                    .load(imageData)
                    .circleCrop()
                    .into(imageButtonProfile)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun signupUser() {
        val birthday = textEditBirthday.text.toString()

        Log.d("Birthday", "$birthday")

        if (!birthday.isEmpty()) {

            AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)
            memeDomuser.birthday = birthday

            val profileImage = imageButtonProfile.drawable
            if (profileImage != null) {
                FireStorageHandler().uploadPhotoWith(memeDomuser.uid, profileImage, {
                    memeDomuser.profilePhoto = it
                    memeDomuser.bio = ""
                    memeDomuser.gallery = listOf()
                    val newUser: HashMap<String, Any> = hashMapOf(
                        "name" to memeDomuser.name,
                        "birthday" to memeDomuser.birthday,
                        "profilePhoto" to memeDomuser.profilePhoto,
                        "uid" to memeDomuser.uid,
                        "gender" to memeDomuser.gender,
                        "email" to memeDomuser.email,
                        "liked" to hashMapOf(memeDomuser.uid to 0),
                        "gallery" to memeDomuser.gallery,
                        "bio" to memeDomuser.bio,
                        "matches" to listOf<String>()
                    )
                    FirestoreHandler().addDataToFirestore("User", memeDomuser.uid, newUser, {
                        progressOverlay.visibility = View.GONE
                        if (it != null) {
                            setupAlertDialog(it)
                        } else {
                            DatabaseManager(this).convertUserObject(memeDomuser, "MainUser")
                            navigateToMain()
                        }
                    })
                })
            }

        } else {
            setupAlertDialog("Missing birthday or username")
        }
    }

    private fun setupTextEdits() {
        editTextPassword.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buttonNextAuth.setBackgroundResource(R.drawable.soft_button)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        textEditUsername.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buttonNextUsername.setBackgroundResource(R.drawable.soft_button)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        textEditBirthday.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buttonNextBirthday.setBackgroundResource(R.drawable.soft_button)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }

}
