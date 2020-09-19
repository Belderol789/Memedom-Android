package com.kratsapps.memedom

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_signup.*
import java.text.SimpleDateFormat
import java.util.*


enum class SignupStates {
    authentication, username, birthday, gender, photo, bio
}

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
        setupActionButtons()
    }

    fun setupUI() {
        val actionBar = supportActionBar
        actionBar!!.title = "Signup"

        val screenWidth = ScreenSize().getScreenWidth()

        Log.d("Screen Size", "ScreenWidth ${screenWidth}")

        val authParams = authentication_layout.layoutParams
        authParams.width = screenWidth

        val usernameParams = username_layout.layoutParams
        usernameParams.width = screenWidth

        val birthdayParams = birthday_layout.layoutParams
        birthdayParams.width = screenWidth

        birthday_input.setRawInputType(InputType.TYPE_NULL)
        signup_scrollview
    }

    private fun setupActionButtons() {
        // Authentication
        if (isEmail) {
            authentication_next_button.setOnClickListener{
                checkIfFieldsHaveValues()
            }
        } else if (memeDomuser != null) {
            // Facebook user
            setupForFacebook(memeDomuser)
            showNameView()
        }

        // Username
        username_next_button.setOnClickListener{
            val username = username_input.text.toString()
            if(!username.isEmpty()) {
                showBirthdayView()
            }
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

    }

    fun checkIfFieldsHaveValues() {
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

    private fun showAuthView() {
        username_layout.visibility = View.GONE
        authentication_layout.visibility = View.VISIBLE
        birthday_layout.visibility = View.GONE
    }

    private fun showNameView() {
        username_layout.visibility = View.VISIBLE
        authentication_layout.visibility = View.GONE
        birthday_layout.visibility = View.GONE
    }

    private fun showBirthdayView() {
        username_layout.visibility = View.GONE
        authentication_layout.visibility = View.GONE
        birthday_layout.visibility = View.VISIBLE
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
