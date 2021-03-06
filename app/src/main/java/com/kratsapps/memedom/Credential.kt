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
import kotlinx.android.synthetic.main.activity_credential.*
import org.json.JSONException


class Credential : AppCompatActivity() {

    var isSignup: Boolean = false
    private lateinit var auth: FirebaseAuth

    lateinit var callbackManager: CallbackManager
    lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credential)
        auth = FirebaseAuth.getInstance()

        isSignup = intent.getBooleanExtra("CREDENTIAL_ACTION", false)
        Log.i("Navigation", "Navigated to Credential with isSignup ${isSignup}")
        setupUI()
    }

    fun setupUI() {
        val actionBar = supportActionBar
        actionBar!!.title = if (isSignup) "Signup" else "Login"

        credential_email.setOnClickListener{
            navigateToSignup(true)
        }

        callbackManager = CallbackManager.Factory.create()
        facebook_login_button.setPermissions("email", "public_profile", "user_birthday")
        facebook_login_button.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("Authentication", "facebook:onSuccess:$loginResult")
                val accessToken = loginResult.accessToken
                getFbInfo()
                handleFacebookAccessToken(accessToken)
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

    private fun getFbInfo() {
        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { `object`, response ->
            try {
                Log.d("Authentication", "fb json object: $`object`")
                Log.d("Authentication", "fb graph response: $response")
                val id = `object`.getString("id")
                val first_name = `object`.getString("first_name")
                val last_name = `object`.getString("last_name")
                val gender = `object`.getString("gender")
                val birthday = `object`.getString("birthday")
                val image_url = "http://graph.facebook.com/$id/picture?type=large"
                val email: String
                if (`object`.has("email")) {
                    email = `object`.getString("email")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val parameters = Bundle()
        parameters.putString(
            "fields",
            "id,first_name,last_name,email,gender,birthday, picture.type(large)"
        ) // id,first_name,last_name,email,gender,birthday,cover, picture.type(large)
        request.parameters = parameters
        request.executeAsync()

        Log.d("Authentication","fb parameters ${parameters}")
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
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Error", "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }

                // ...
            }
    }

    fun updateUI(user: FirebaseUser?) {
        // Proceed to next signup steps
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun navigateToSignup(isEmail: Boolean) {
        val intent: Intent = Intent(this, Signup::class.java)
        intent.putExtra("AUTH_METHOD", isEmail)
        startActivity(intent)
    }

}