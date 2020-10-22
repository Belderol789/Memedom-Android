package com.kratsapps.memedom

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.facebook.FacebookSdk
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.fragments.*
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = NotificationsFragment()
    val msgFragment = MessagesFragment()
    val createFragment = CreateFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activateFacebook()
        setupBottomNavigation()
        checkLikedStatus()
    }

    private fun setupBottomNavigation() {
        navigationBottom.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.ic_home -> makeCurrentFragment(homeFragment)
                R.id.ic_profile -> makeCurrentFragment(profileFragment)
                R.id.ic_create -> makeCurrentFragment(createFragment)
                R.id.ic_notifs -> makeCurrentFragment(notifFragment)
                R.id.ic_chat -> makeCurrentFragment(msgFragment)
            }
            true
        }
        makeCurrentFragment(homeFragment)
    }

    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.wrapperFL, fragment)
            commit()
        }

    private fun activateFacebook() {
        FacebookSdk.fullyInitialize()
    }

    private fun checkLikedStatus() {
        val user = FirebaseAuth.getInstance().getCurrentUser()
        val mainUID = DatabaseManager(this).getMainUserID()
        if(user != null) {
            FirestoreHandler().checkMatchingStatus(user.uid)
        } else if (mainUID != null) {
            FirestoreHandler().checkMatchingStatus(mainUID)
        }
    }

    private fun checkLoginStatus() {
        val user = FirebaseAuth.getInstance().getCurrentUser()
        setUIForUser(user)

        val firebaseAuth = FirebaseAuth.getInstance()
        val mAuthListener = FirebaseAuth.AuthStateListener() {
            fun onAuthStateChanged(@NonNull firebaseAuth: FirebaseAuth) {
                val user = FirebaseAuth.getInstance().getCurrentUser()
                if (user != null) {
                    setUIForUser(user)
                }
            }
        }
    }

    private fun setUIForUser(user: FirebaseUser?) {
        if (user != null) {
            navigationBottom.visibility = View.VISIBLE
            makeCurrentFragment(homeFragment)
        } else {
            navigationBottom.visibility = View.GONE
        }
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