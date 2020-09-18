package com.kratsapps.memedom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_signup.*


class Signup : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    lateinit var memeDomuser: MemeDomUser
    lateinit var progressOverlay: View

    private val mAuth: FirebaseAuth? = null
    var isEmail: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        memeDomuser = intent.extras?.get("MEMEDOM_USER") as MemeDomUser
        isEmail = intent.getBooleanExtra("AUTH_METHOD", true)

        Log.i("Navigation", "Navigated to Signup")
        setupUI()
    }

    fun setupUI() {
        val actionBar = supportActionBar
        actionBar!!.title = "Signup"

        if (isEmail) {
            next_authentication.setOnClickListener{
                hideKeyboard()

                val email = email_input.text.toString()
                val password = password_input.text.toString()

                checkIfFieldsHaveValues {
                    if (it) {
                        Log.d("Authentication", "Fields checked")
                        userDidAuthEmail(email, password)
                    }
                }
            }
        } else if (memeDomuser != null) {
            // Facebook user
            setupForFacebook(memeDomuser)
            showNameView()
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

        progressOverlay = findViewById(R.id.progress_overlay)
        AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Authentication", "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        progressOverlay.visibility = View.GONE
                        memeDomuser.uid = user.uid
                        showNameView()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Authentication", "createUserWithEmail:failure", task.exception)
                    setupAlertDialog(task.exception?.message)
                }
                // ...
            }
    }

    private fun setupForFacebook(user: MemeDomUser) {
        username_input.setText(user.name)
    }

    private fun showNameView() {
        username_layout.visibility = View.VISIBLE
        authentication_layout.visibility = View.GONE
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
}
