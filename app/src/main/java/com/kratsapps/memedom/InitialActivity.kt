package com.kratsapps.memedom

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_initial.*

class InitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)

        signupBtn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            this.startActivity(intent)
            finish()
        }

        loginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            this.startActivity(intent)
            finish()
        }

        guestBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            this.startActivity(intent)
            finish()
        }

        val user = FirebaseAuth.getInstance().getCurrentUser()
        if (user != null) {
            val intent: Intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}