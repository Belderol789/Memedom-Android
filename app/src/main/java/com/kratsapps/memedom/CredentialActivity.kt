package com.kratsapps.memedom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.AndroidUtils
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import kotlinx.android.synthetic.main.activity_credential.*
import org.json.JSONException
import java.io.Serializable


class CredentialActivity : AppCompatActivity() {

    var memeDomUser: MemeDomUser = MemeDomUser()
    var userSignup: Boolean = false
    private lateinit var auth: FirebaseAuth

    lateinit var progressOverlay: View
    lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credential)
        auth = FirebaseAuth.getInstance()
        userSignup = intent.getBooleanExtra("CREDENTIAL_ACTION", false)
        Log.i("Navigation", "Navigated to Credential with isSignup ${userSignup}")
        setupUI()
    }

    fun setupUI() {
        progressOverlay = findViewById(R.id.progress_overlay)
        buttonEmail.setOnClickListener{
            if(userSignup) {
                navigateToSignup(true)
            } else {
                navigateToLogin()
            }
        }

        callbackManager = CallbackManager.Factory.create()
        buttonFacebook.setPermissions("email", "public_profile", "user_birthday")
        buttonFacebook.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("Authentication", "facebook:onSuccess:$loginResult")
                val accessToken = loginResult.accessToken
                getFbInfo {
                    handleFacebookAccessToken(accessToken)
                }
            }

            override fun onCancel() {
                Log.d("Authentication", "facebook:onCancel")
                // ...
            }

            override fun onError(error: FacebookException) {
                Log.d("Authentication", "facebook:onError", error)
                Toast.makeText(
                    baseContext,
                    "We're sorry, there was an error with your signup",
                    Toast.LENGTH_SHORT
                ).show()
                // ...
            }
        })
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
                memeDomUser.profilePhoto = url

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
        Log.d("Facebook","fb parameters ${parameters}")
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("Authentication", "handleFacebookAccessToken:$token")
        AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Error", "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // ...
            }
    }

    fun updateUI(firebaseUser: FirebaseUser?) {
        Log.d("Firestore", "Adding New User")
        if (firebaseUser != null) {
            memeDomUser.uid = firebaseUser.uid
            if(userSignup) {
                progressOverlay.visibility = View.GONE
                navigateToSignup(false)
            } else {
                FirestoreHandler().getUserDataWith(memeDomUser.uid, {
                    DatabaseManager(this).convertUserObject(it, "MainUser")
                    progressOverlay.visibility = View.GONE
                    val intent: Intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun navigateToLogin() {
        val intent: Intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("MEMEDOM_USER", memeDomUser as Serializable)
        startActivity(intent)
    }

    private fun navigateToSignup(userEmail: Boolean) {
        val intent: Intent = Intent(this, SignupActivity::class.java)
        intent.putExtra("AUTH_METHOD", userEmail)
        if(memeDomUser.uid != null) {
            intent.putExtra("MEMEDOM_USER", memeDomUser as Serializable)
        }
        startActivity(intent)
    }
}