package com.kratsapps.memedom

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_credential.view.*
import kotlinx.android.synthetic.main.activity_signup.*
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import kotlin.concurrent.schedule

class Signup : AppCompatActivity() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    private var screenWidth: Int = 0

    private lateinit var auth: FirebaseAuth
    lateinit var memeDomuser: MemeDomUser
    lateinit var progressOverlay: View

    private val mAuth: FirebaseAuth? = null
    var isEmail: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        requestPermissionToPhotos()
        auth = FirebaseAuth.getInstance()
        isEmail = intent.getBooleanExtra("AUTH_METHOD", true)

        Log.i("Navigation", "Navigated to Signup")
        setupUI()
        setupActionButtons()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 121 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            imageButton_profile.isClickable = true
        }
    }

    private fun requestPermissionToPhotos() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,Array(1){android.Manifest.permission.READ_EXTERNAL_STORAGE}, 121)
        } else {
            imageButton_profile.isClickable = true
        }
    }

    private fun setupUI() {

        val actionBar = supportActionBar
        actionBar!!.title = "Signup"

        progressOverlay = findViewById(R.id.progress_overlay)

        screenWidth = ScreenSize().getScreenWidth()

        Log.d("Screen Size", "ScreenWidth ${screenWidth}")

        val authParams = authentication_layout.layoutParams
        authParams.width = screenWidth

        val usernameParams = username_layout.layoutParams
        usernameParams.width = screenWidth

        val birthdayParams = birthday_layout.layoutParams
        birthdayParams.width = screenWidth

        birthday_input.setRawInputType(InputType.TYPE_NULL)
        scrollViewSignup.setOnTouchListener(View.OnTouchListener { v, event -> true })

        //For Facebook
        if (!isEmail) {
            memeDomuser = intent.extras?.get("MEMEDOM_USER") as MemeDomUser
            authentication_layout.visibility = View.INVISIBLE
            username_input.setText(memeDomuser.name)
            birthday_input.setText(memeDomuser.birthday)
            Glide.with(this)
                .load(memeDomuser.profilePhoto)
                .centerCrop()
                .into(imageButton_profile)

            Timer().schedule(1000) {
                scrollViewSignup.smoothScrollTo(screenWidth, 0)
            }
        } else {
            memeDomuser = MemeDomUser()
        }
    }

    private fun setupActionButtons() {
        // Authentication
        if (isEmail) {
            authentication_next_button.setOnClickListener{
                checkIfFieldsHaveValues()
            }
        }

        // Username
        username_next_button.setOnClickListener{
            val username = username_input.text.toString()
            if(!username.isEmpty()) {
                memeDomuser.name = username
                scrollViewSignup.smoothScrollTo(screenWidth * 2, 0)
            }
        }

        imageButton_profile.setOnClickListener{
            prepOpenImageGallery()
        }

        val dateText: String = SimpleDateFormat("dd MMMM yyyy").format(System.currentTimeMillis())
        birthday_input.setText(dateText)

        var cal = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val myFormat = "dd.MM.yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            birthday_input.setText(sdf.format(cal.time))
        }

        birthday_input.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        birthday_next_button.setOnClickListener{

            val birthday = birthday_input.text.toString()

            Log.d("Birthday", "$birthday")

            if (!birthday.isEmpty()) {

                AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)
                memeDomuser.birthday = birthday

                val bitmap = (imageButton_profile.drawable as BitmapDrawable).bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                FirestoreHandler().addUserToDatabase(memeDomuser, {
                    if (it == null) {
                        Log.d("Firestore", "User has data has been saved to firestore")
                        navigateToMain()
                    } else {
                        setupAlertDialog(it)
                    }
                    progressOverlay.visibility = View.GONE
                })

                FireStorageHandler().uploadPhotoWith(memeDomuser.uid, data, {
                    if (it != null) {
                        FirestoreHandler().updateUserDatabase(memeDomuser.uid, "profilePhoto", it)
                    } else {
                        setupAlertDialog("Error with saving your profile photo :(")
                    }
                    progressOverlay.visibility = View.GONE
                })
            } else {
                setupAlertDialog("Missing birthday or username")
            }
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

        val email = email_input.text.toString()
        val password = password_input.text.toString()

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
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {
                val imageData = data.data
                Glide.with(this)
                    .load(imageData)
                    .centerCrop()
                    .into(imageButton_profile)
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

}
