package com.kratsapps.memedom

import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activateFacebook()
        setContentView(R.layout.activity_main)
        setupTabLayout()
    }

    private fun setupTabLayout() {
        viewPager.adapter = PageAdapter(this, supportFragmentManager)
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_action_name)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_action_home)
        tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_action_notif)
        tabLayout.getTabAt(3)?.setIcon(R.drawable.ic_action_chat)
    }

    private fun activateFacebook() {
        FacebookSdk.fullyInitialize()
    }

}