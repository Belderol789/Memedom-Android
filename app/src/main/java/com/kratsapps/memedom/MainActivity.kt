package com.kratsapps.memedom

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.facebook.FacebookSdk
import com.kratsapps.memedom.fragments.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activateFacebook()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val profileFragment = ProfileFragment()
        val homeFragment = HomeFragment()
        val notifFragment = NotificationsFragment()
        val msgFragment = MessagesFragment()
        val settingFragment = SettingFragment()

        makeCurrentFragment(homeFragment)

        navigationBottom.visibility = View.GONE

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

}