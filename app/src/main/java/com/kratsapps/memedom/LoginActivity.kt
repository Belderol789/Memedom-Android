package com.kratsapps.memedom

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    lateinit var callbackManager: CallbackManager


    var memeDomUser: MemeDomUser = MemeDomUser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)

        loginBackBtn.setOnClickListener {
            onBackPressed()
        }

         buttonNextLoginAuth.setOnClickListener {
            checkIfFieldsHaveValues()
        }

        setupFacebookLogin()

    }

    //Email~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun userDidLoginEmail(email: String, password: String) {
        Log.d("Firestore Login", "Logging in as $email")

        loginLoadingView.visibility = View.VISIBLE

        DatabaseManager(this).saveToPrefsInt("minAge", 16)
        DatabaseManager(this).saveToPrefsInt("maxAge", 65)
        DatabaseManager(this).clearPostIDs()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Firestore Login", "signInWithEmail:success")
                    loginSuccess(auth.currentUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Firestore Login", "signInWithEmail:failure", task.exception)
                    loginLoadingView.visibility = View.INVISIBLE
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
    }

    private fun checkIfFieldsHaveValues() {
        hideKeyboard()
        val email = editTextLoginEmail.text.toString()
        val password = editTextLoginPassword.text.toString()

        if(email.isEmpty() || password.toString().isEmpty() || password.toString().length < 6) {
            Toast.makeText(baseContext, "Email or Password is invalid", Toast.LENGTH_SHORT).show()
        } else {
            userDidLoginEmail(email, password)
        }
    }

    private fun loginSuccess(firebaseUser: FirebaseUser?) {
        if (firebaseUser?.uid != null) {
            FirestoreHandler().getUserDataWith(firebaseUser.uid, {
                Log.d("Firestore Login", "Logged in as $it")
                loginLoadingView.visibility = View.INVISIBLE
                DatabaseManager(this).convertUserObject(it, "MainUser", {
                    val intent: Intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                })
            })
        }
    }
    //Email~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //Facebook~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun setupFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()

        val facebookBtn = findViewById<Button>(R.id.facebookBtn)
        val fbLoginBtn = findViewById<LoginButton>(R.id.fbLoginBtn)

        fbLoginBtn.setPermissions("email", "public_profile")
        fbLoginBtn.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("Authentication", "facebook:onSuccess:$loginResult")
                getFbInfo {
                    handleFacebookAccessToken(loginResult.accessToken)
                }
            }

            override fun onCancel() {
                Log.d("Authentication", "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d("Authentication", "facebook:onError", error)
                Toast.makeText(
                    baseContext,
                    "We're sorry, there was an error with your facebook request",
                    Toast.LENGTH_SHORT
                ).show()
                // ...
            }
        })

        facebookBtn.setOnClickListener {
            loginLoadingView.visibility = View.VISIBLE
            fbLoginBtn.performClick()
        }
    }

    private fun getFbInfo(callback: () -> Unit) {
        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { `object`, response ->
            try {
                Log.d("Facebook", "Facebook Object $`object`")
                memeDomUser.birthday = `object`.getString("birthday")
                memeDomUser.name = `object`.getString("first_name")

                val picture = `object`.getJSONObject("picture")
                val data = picture.getJSONObject("data")
                val url = data.getString("url")
                memeDomUser.profilePhoto = url.replace("\\/", "", false)

                Log.d("Facebook", "Facebook profile photo ${memeDomUser.profilePhoto}")

                val email: String
                if (`object`.has("email")) {
                    memeDomUser.email = `object`.getString("email")
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
                    loginSuccess(auth.currentUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Error", "signInWithCredential:failure", task.exception)
                    loginLoadingView.visibility = View.INVISIBLE
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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

}