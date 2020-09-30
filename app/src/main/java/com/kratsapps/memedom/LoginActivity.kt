package com.kratsapps.memedom

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    var isEmail: Boolean = true
    private lateinit var auth: FirebaseAuth
    lateinit var memeDomuser: MemeDomUser
    lateinit var progressOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        progressOverlay = findViewById(R.id.progress_overlay)

        buttonNextLoginAuth.setOnClickListener {
            checkIfFieldsHaveValues()
        }
    }

    private fun userDidLoginEmail(email: String, password: String) {
        if(!email.isEmpty() && !password.isEmpty()) {

            AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Firestore Login", "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user!!)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Firestore Login", "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }

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

    private fun updateUI(firebaseUser: FirebaseUser) {
        FirestoreHandler().getUserDataWith(firebaseUser.uid, {
            DatabaseManager(this).convertUserObject(it, "MainUser")
            navigateToMain()
        })
        progressOverlay.visibility = View.GONE
    }

    private fun navigateToMain() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}