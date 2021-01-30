package com.kratsapps.memedom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.firebaseutils.FireStorageHandler
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_signup.*
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*


class SignupActivity : AppCompatActivity() {

    private val IMAGE_GALLERY_REQUEST_CODE: Int = 2001
    private lateinit var auth: FirebaseAuth

    lateinit var callbackManager: CallbackManager

    var passwordIsHidden: Boolean = false

    var hasProfilePhoto = false
    var memeDomuser: MemeDomUser = MemeDomUser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()

        DatabaseManager(this).saveToPrefsBoolean("TutorialKey", false)
        DatabaseManager(this).convertUserObject(null,  {})
        LoginManager.getInstance().logOut()
        FirebaseAuth.getInstance().signOut()

        Log.i("Navigation", "Navigated to Signup")
        setupUI()
        setupActionButtons()
        setupFacebookSignup()
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
            profilePhotoBtn.isClickable = true
        }
    }

    private fun setupUI() {
        otherDetailsCard.visibility = View.INVISIBLE
        privacyView.visibility = View.INVISIBLE
        datePickerContainer.visibility = View.INVISIBLE

        editTextSignupBirthday.setRawInputType(InputType.TYPE_NULL)
        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)
    }

    private fun setupActionButtons() {
        // Authentication
        editTextSignupBirthday.setText("")
        var cal = Calendar.getInstance()

        birthDoneBtn.visibility = View.INVISIBLE
        birthDoneBtn.setOnClickListener {
            datePickerContainer.visibility = View.INVISIBLE
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            birthdayPicker.setOnDateChangedListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val myFormat = "MM/dd/yyyy" // mention the format you need
                val sdf = SimpleDateFormat(myFormat, Locale.US)
                editTextSignupBirthday.setText(sdf.format(cal.time))

                val birthday = editTextSignupBirthday.text.toString()
                memeDomuser.birthday = birthday

                if (memeDomuser.getUserAge() >= 16) {
                    birthDoneBtn.visibility = View.VISIBLE
                } else {
                    birthDoneBtn.visibility = View.INVISIBLE
                }
            }
        }

        passStateBtn.setOnClickListener {
            if (passwordIsHidden) {
                passStateBtn.setImageResource(R.drawable.ic_action_password_hide)
                editTextSignupnPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                passStateBtn.setImageResource(R.drawable.ic_action_password_show)
                editTextSignupnPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            }
            passwordIsHidden = !passwordIsHidden
        }

        editTextSignupBirthday.setOnClickListener {
            datePickerContainer.visibility = View.VISIBLE
        }

        privacyBtn.setOnClickListener {
            proceedToPrivacy()
        }

        removePolicyBtn.setOnClickListener {
            privacyView.visibility = View.INVISIBLE
        }

        profilePhotoBtn.setOnClickListener {
            prepOpenImageGallery()
        }

        buttonNextSignupAuth.setOnClickListener {
            checkIfFieldsHaveValues()
        }

        signupBackBtn.setOnClickListener {
            onBackPressed()
        }

        signupGenderMale.setOnClickListener {
            activateFilter(signupGenderMale, "Male", null,
                listOf(
                    signupGenderFemale,
                    signupGenderOther))
        }

        signupGenderFemale.setOnClickListener {
            activateFilter(signupGenderFemale, "Female", null,
                listOf(signupGenderMale,
                    signupGenderOther))
        }

        signupGenderOther.setOnClickListener {
            activateFilter(signupGenderOther, "Other", null,
                listOf(
                    signupGenderFemale,
                    signupGenderMale))
        }

        signupLookingMale.setOnClickListener {
            activateFilter(
                signupLookingMale, null, "Male", listOf(
                    signupLookingFemale,
                    signupLookingOther
                )
            )
        }

        signupLookingFemale.setOnClickListener {
            activateFilter(
                signupLookingFemale, null, "Female", listOf(
                    signupLookingMale,
                    signupLookingOther
                )
            )
        }

        signupLookingOther.setOnClickListener {
            activateFilter(
                signupLookingOther, null, "Other", listOf(
                    signupLookingMale,
                    signupLookingFemale
                )
            )
        }

        signupFinishBtn.setOnClickListener {
            val username = editTextSignupUsername.text.toString()
            FirestoreHandler().checkUsernameAvailability(username, {
                Log.d("Usernames", "Username is available $it")
                if (it) {
                    memeDomuser.name = username
                    if (memeDomuser.getUserAge() <= 13) {
                        Toast.makeText(baseContext, "Sorry, you're too young", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        saveUserImage()
                    }
                } else {
                    Toast.makeText(baseContext, "Username Taken", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun proceedToPrivacy() {
        privacyView.visibility = View.VISIBLE
    }

    private fun activateFilter(
        active: AppCompatRadioButton,
        gender: String?,
        lookingFor: String?,
        deactives: List<AppCompatRadioButton>
    ) {
        for (segment in deactives) {
            segment.isChecked = false
        }

        active.isChecked = true

        if (gender != null) {
            memeDomuser.gender = gender
        }

        if (lookingFor != null) {
            memeDomuser.lookingFor = lookingFor
        }
    }

    private fun navigateToMain() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun prepOpenImageGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    private fun checkIfFieldsHaveValues() {
        hideKeyboard()
        val checkBox = privacyCheckBox.isChecked
        val email = editTextSignupEmail.text.toString()
        val password = editTextSignupnPassword.text.toString()
        val birthday = editTextSignupBirthday.text.toString()
        memeDomuser.birthday = birthday

        if (!checkBox) {
            Toast.makeText(baseContext, "Kindly agree to the Privacy Policy", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6 ) {
            Toast.makeText(baseContext, "Password is too short", Toast.LENGTH_SHORT).show()
        } else if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "Some fields are missing", Toast.LENGTH_SHORT).show()
        } else {
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
                        showCardView()
                        memeDomuser.uid = user.uid
                        memeDomuser.email = email
                        requestPermissionToPhotos()
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

    private fun saveUserImage() {
        signupLoadingView.visibility = View.VISIBLE
        if (hasProfilePhoto) {
            FireStorageHandler().uploadPhotoWith(
                memeDomuser.uid,
                profilePhotoBtn.drawable,
                { profilePhotoURL ->
                    memeDomuser.profilePhoto = profilePhotoURL
                    signupUser()
                })
        } else {
            signupUser()
        }
    }

    private fun signupUser() {
        if (!memeDomuser.birthday.isEmpty()) {
            memeDomuser.bio = ""
            memeDomuser.gallery = listOf()
            memeDomuser.rejects = listOf()
            memeDomuser.matches = listOf(memeDomuser.uid)
            memeDomuser.seenOldMemes = listOf()
            memeDomuser.dateJoined = System.currentTimeMillis()
            memeDomuser.pendingMatches = listOf()

            val newUser: HashMap<String, Any> = hashMapOf(
                "name" to memeDomuser.name,
                "birthday" to memeDomuser.birthday,
                "profilePhoto" to memeDomuser.profilePhoto,
                "uid" to memeDomuser.uid,
                "gender" to memeDomuser.gender,
                "lookingFor" to memeDomuser.lookingFor,
                "email" to memeDomuser.email,
                "dating" to hashMapOf(memeDomuser.uid to 0),
                "gallery" to memeDomuser.gallery,
                "bio" to memeDomuser.bio,
                "matches" to memeDomuser.matches,
                "rejects" to memeDomuser.rejects,
                "seenOldMemes" to memeDomuser.seenOldMemes,
                "dateJoined" to memeDomuser.dateJoined,
                "pendingMatches" to memeDomuser.pendingMatches,
                "minAge" to 16,
                "maxAge" to 65
            )

            FirestoreHandler().addDataToFirestore("User", memeDomuser.uid, newUser, {
                signupLoadingView.visibility = View.INVISIBLE
                if (it != null) {
                    signupLoadingView.visibility = View.INVISIBLE
                    setupAlertDialog(it)
                } else {
                    signupLoadingView.visibility = View.INVISIBLE
                    DatabaseManager(this).convertUserObject(memeDomuser, {
                        navigateToMain()
                    })
                }
            })

            FirestoreHandler().addDataToFirestore(
                "Username",
                memeDomuser.uid,
                hashMapOf("Username" to memeDomuser.name),
                {})

        } else {
            signupLoadingView.visibility = View.INVISIBLE
            setupAlertDialog("Missing birthday or too young")
        }
    }

    private fun generateRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { charset.random() }
            .joinToString("")
    }

    //Facebook~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun setupFacebookSignup() {
        callbackManager = CallbackManager.Factory.create()

        fbSignupBtn.setPermissions("email", "public_profile")
        fbSignupBtn.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("Authentication", "facebook:onSuccess:$loginResult")
                signupLoadingView.visibility = View.VISIBLE
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
            fbSignupBtn.performClick()
        }
    }

    private fun getFbInfo(callback: () -> Unit) {
        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { `object`, response ->
            try {
                Log.d("Facebook", "Facebook Object $`object`")
                memeDomuser.name = `object`.getString("first_name")
                Log.d("Facebook", "Facebook username ${memeDomuser.name}")
                Log.d("Facebook", "Facebook username ${editTextSignupUsername.text.toString()}")

                val picture = `object`.getJSONObject("picture")
                val data = picture.getJSONObject("data")
                val url = data.getString("url")

                memeDomuser.profilePhoto = url.replace("\\/", "", false)

                Log.d("Facebook", "Facebook profile photo ${memeDomuser.profilePhoto}")

                val email: String
                if (`object`.has("email")) {
                    memeDomuser.email = `object`.getString("email")
                }
                memeDomuser.birthday = `object`.getString("birthday")
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
        signupLoadingView.visibility = View.INVISIBLE
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("Authentication", "handleFacebookAccessToken:$token")
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "signInWithCredential:success")
                    signupLoadingView.visibility = View.INVISIBLE
                    val user = auth.currentUser
                    if (user != null) {
                        showCardView()
                        memeDomuser.uid = user.uid

                        editTextSignupUsername.setText(memeDomuser.name)
                        Glide.with(this)
                            .load(memeDomuser.profilePhoto)
                            .error(ContextCompat.getDrawable(this, R.drawable.ic_action_name))
                            .circleCrop()
                            .into(profilePhotoBtn)
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
            .addOnFailureListener {
                signupLoadingView.visibility = View.INVISIBLE
            }
            .addOnCanceledListener {
                signupLoadingView.visibility = View.INVISIBLE
            }
            .addOnSuccessListener {
                signupLoadingView.visibility = View.INVISIBLE
            }
    }



    private fun showCardView() {
        signupBackBtn.visibility = View.GONE
        signupLoadingView.visibility = View.INVISIBLE
        otherDetailsCard.visibility = View.VISIBLE
        profilePhotoBtn.isClickable = true
    }

    //Facebook~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_REQUEST_CODE && data != null && data.data != null) {
                val imageData = data.data
                hasProfilePhoto = true
                profilePhotoBtn.background = null
                profilePhotoBtn.setColorFilter(Color.parseColor("#00ff0000"))
                Glide.with(this)
                    .load(imageData)
                    .circleCrop()
                    .into(profilePhotoBtn)
            }
        }
    }
}
