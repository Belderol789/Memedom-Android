package com.kratsapps.memedom

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.facebook.FacebookSdk
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kratsapps.memedom.fragments.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val profileFragment = ProfileFragment()
    val homeFragment = HomeFragment()
    val notifFragment = NotificationsFragment()
    val msgFragment = MessagesFragment()
    val settingFragment = SettingFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activateFacebook()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        makeCurrentFragment(homeFragment)

        navigationBottom.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.ic_home -> makeCurrentFragment(homeFragment)
                R.id.ic_profile -> makeCurrentFragment(profileFragment)
                R.id.ic_notifs -> makeCurrentFragment(notifFragment)
                R.id.ic_chat -> makeCurrentFragment(msgFragment)
                R.id.ic_settings -> makeCurrentFragment(settingFragment)
            }
            true
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.wrapperFL, fragment)
            commit()
        }

    private fun activateFacebook() {
        FacebookSdk.fullyInitialize()
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

}