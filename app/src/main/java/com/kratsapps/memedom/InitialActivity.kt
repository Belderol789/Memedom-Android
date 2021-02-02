package com.kratsapps.memedom

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_initial.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class InitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
        FirestoreHandler().setupFirestore()

        val user = FirebaseAuth.getInstance().getCurrentUser()
        val savedUser = DatabaseManager(this).retrieveSavedUser()

        Log.d("LoggingIn", "Firebase $user Local $savedUser")

        if (user != null && savedUser != null) {
            val intent: Intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        signupBtn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            this.startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            this.startActivity(intent)
        }

        guestBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            this.startActivity(intent)
            finish()
        }
    }
}