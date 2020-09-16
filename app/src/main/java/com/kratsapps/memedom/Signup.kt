package com.kratsapps.memedom

import android.icu.util.TimeUnit
import android.opengl.Visibility
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_signup.*
import javax.xml.datatype.DatatypeConstants.SECONDS

class Signup : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()
        Log.i("Navigation", "Navigated to Signup")
        setupUI()
    }

    fun setupUI() {
        val actionBar = supportActionBar
        actionBar!!.title = "Signup"

        next_authentication.setOnClickListener{
            hideKeyboard()

            val email = email_input.text.toString()
            val password = password_input.text.toString()

            checkIfFieldsHaveValues {
                if (it) {
                    userDidAuthEmail(email, password)
                }
            }
        }
    }

    fun checkIfFieldsHaveValues(passed: (Boolean) -> Unit) {
        if(email_input.text.toString().isEmpty() || password_input.text.toString().isEmpty() || password_input.text.toString().length < 6) {
            passed(false)
            Toast.makeText(baseContext, "Email or Password is invalid", Toast.LENGTH_SHORT).show()
        } else {
            passed(true)
        }
    }

    private fun userDidAuthEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Authentication", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
                // ...
            }
    }

    fun updateUI(user: FirebaseUser?) {
        // Proceed to next signup steps
    }

}