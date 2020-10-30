package com.kratsapps.memedom

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.utils.AndroidUtils
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import com.kratsapps.memedom.utils.hideKeyboard
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

        Log.d("Firestore Login", "Logging in as $email")

        if(!email.isEmpty() && !password.isEmpty()) {

            AndroidUtils().animateView(progressOverlay, View.VISIBLE, 0.4f, 200)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Firestore Login", "signInWithEmail:success")
                        val user = auth.currentUser
                        if(user != null) {
                            updateUI(user)
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Firestore Login", "signInWithEmail:failure", task.exception)
                        progressOverlay.visibility = View.GONE
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

            Log.d("Firestore Login", "Logged in as $it")

            progressOverlay.visibility = View.GONE
            DatabaseManager(this).convertUserObject(it, "MainUser")
            navigateToMain()
        })
    }

    private fun navigateToMain() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

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