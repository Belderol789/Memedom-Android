package com.kratsapps.memedom

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_signup.*
import kotlinx.android.synthetic.main.activity_signup.loadingImageView
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*

class SignupActivity : AppCompatActivity() {


    private val defaultPhotoURL: String = "https://firebasestorage.googleapis.com/v0/b/memedom-fb37b.appspot.com/o/AppSettings%2Fmemedom%20icon.png?alt=media&token=e29c4cae-3a13-47fb-b3c0-0136445a45cf"
    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    private lateinit var auth: FirebaseAuth

    lateinit var callbackManager: CallbackManager

    var memeDomuser: MemeDomUser = MemeDomUser()
    var userEmail: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()
        userEmail = intent.getBooleanExtra("AUTH_METHOD", true)

        Log.i("Navigation", "Navigated to Signup")
        setupUI()
        setupActionButtons()
        setupFacebookSignup()
    }

    private fun setupUI() {
        editTextSignupBirthday.setRawInputType(InputType.TYPE_NULL)
        memeDomuser.gender = "Other"

        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)
    }

    private fun setupActionButtons() {
        // Authentication
        editTextSignupBirthday.setText("")

        var cal = Calendar.getInstance()

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val myFormat = "MM/dd/yyyy" // mention the format you need
                val sdf = SimpleDateFormat(myFormat, Locale.US)
                editTextSignupBirthday.setText(sdf.format(cal.time))
            }

        editTextSignupBirthday.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonNextSignupAuth.setOnClickListener {
            checkIfFieldsHaveValues()
        }

        signupBackBtn.setOnClickListener {
            onBackPressed()
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

        val username = editTextSignupUsername.text.toString()
        val email = editTextSignupEmail.text.toString()
        val password = editTextSignupnPassword.text.toString()
        val birthday = editTextSignupBirthday.text.toString()

        val checkBox = privacyCheckBox.isChecked

        memeDomuser.birthday = birthday

        if (!checkBox) {
            Toast.makeText(baseContext, "Kindly agree to the Privacy Policy", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6 ) {
            Toast.makeText(baseContext, "Password is too short", Toast.LENGTH_SHORT).show()
        } else if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(baseContext, "Some fields are missing", Toast.LENGTH_SHORT).show()
        } else if (memeDomuser.getUserAge() <= 13) {
            Toast.makeText(baseContext, "Sorry, you're too young", Toast.LENGTH_SHORT).show()
        } else {
            memeDomuser.name = username
            userDidAuthEmail(email, password)
        }
    }

    private fun userDidAuthEmail(email: String, password: String) {
        signupLoadingView.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        memeDomuser.uid = user.uid
                        memeDomuser.email = email
                        signupUser()
                    }
                } else {
                    signupLoadingView.visibility = View.INVISIBLE
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun signupUser() {
        if (!memeDomuser.birthday.isEmpty()) {

            DatabaseManager(this).clearPostIDs()

            if (memeDomuser.profilePhoto.isEmpty()) {
                memeDomuser.profilePhoto = defaultPhotoURL
            }

            memeDomuser.bio = ""
            memeDomuser.gallery = listOf()
            memeDomuser.rejects = listOf()
            memeDomuser.memes = listOf()
            memeDomuser.matches = listOf(memeDomuser.uid)

            val newUser: HashMap<String, Any> = hashMapOf(
                "name" to memeDomuser.name,
                "birthday" to memeDomuser.birthday,
                "profilePhoto" to memeDomuser.profilePhoto,
                "uid" to memeDomuser.uid,
                "gender" to "",
                "lookingFor" to "",
                "minAge" to 16,
                "maxAge" to 65,
                "email" to memeDomuser.email,
                "liked" to hashMapOf(memeDomuser.uid to 0),
                "gallery" to memeDomuser.gallery,
                "bio" to memeDomuser.bio,
                "matches" to memeDomuser.matches,
                "rejects" to memeDomuser.rejects,
                "memes" to memeDomuser.memes
            )
            FirestoreHandler().addDataToFirestore("User", memeDomuser.uid, newUser, {
                signupLoadingView.visibility = View.INVISIBLE
                if (it != null) {
                    signupLoadingView.visibility = View.INVISIBLE
                    setupAlertDialog(it)
                } else {
                    signupLoadingView.visibility = View.INVISIBLE
                    DatabaseManager(this).convertUserObject(memeDomuser, "MainUser", {
                        navigateToMain()
                    })
                }
            })

        } else {
            signupLoadingView.visibility = View.INVISIBLE
            setupAlertDialog("Missing birthday or too young")
        }
    }

    //Facebook~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun setupFacebookSignup() {
        callbackManager = CallbackManager.Factory.create()

        fbSignupBtn.setPermissions("email", "public_profile")
        fbSignupBtn.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("Authentication", "facebook:onSuccess:$loginResult")
                getFbInfo {
                    handleFacebookAccessToken(loginResult.accessToken)
                }
            }

            override fun onCancel() {
                signupLoadingView.visibility = View.INVISIBLE
                Log.d("Authentication", "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                signupLoadingView.visibility = View.INVISIBLE
                Log.d("Authentication", "facebook:onError", error)
                Toast.makeText(
                    baseContext,
                    "We're sorry, there was an error with your facebook request",
                    Toast.LENGTH_SHORT
                ).show()
                // ...
            }
        })

        facebookSignupBtn.setOnClickListener {
            signupLoadingView.visibility = View.VISIBLE
            fbSignupBtn.performClick()
        }
    }

    private fun getFbInfo(callback: () -> Unit) {
        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { `object`, response ->
            try {
                Log.d("Facebook", "Facebook Object $`object`")
                memeDomuser.birthday = `object`.getString("birthday")
                memeDomuser.name = `object`.getString("first_name")

                val picture = `object`.getJSONObject("picture")
                val data = picture.getJSONObject("data")
                val url = data.getString("url")
                memeDomuser.profilePhoto = url.replace("\\/", "", false)

                Log.d("Facebook", "Facebook profile photo ${memeDomuser.profilePhoto}")

                val email: String
                if (`object`.has("email")) {
                    memeDomuser.email = `object`.getString("email")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val parameters = Bundle()
        parameters.putString(
            "fields",
            "id,first_name,email,gender,birthday, picture.type(large)"
        ) // id,first_name,last_name,email,gender,birthday,cover, picture.type(large)
        request.parameters = parameters
        request.executeAsync()
        callback.invoke()
        Log.d("Facebook", "fb parameters ${parameters}")
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("Authentication", "handleFacebookAccessToken:$token")
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        memeDomuser.uid = user.uid
                        signupUser()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Error", "signInWithCredential:failure", task.exception)
                    signupLoadingView.visibility = View.INVISIBLE
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // ...
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    //Facebook~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

}

/*
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 121 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            imageButtonProfile.isClickable = true
        }
    }

    private fun requestPermissionToPhotos() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                Array(1) { android.Manifest.permission.READ_EXTERNAL_STORAGE },
                121
            )
        } else {
            imageButtonProfile.isClickable = true
        }
    }

editTextPassword.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buttonNextAuth.setBackgroundResource(R.drawable.soft_button)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
 */
