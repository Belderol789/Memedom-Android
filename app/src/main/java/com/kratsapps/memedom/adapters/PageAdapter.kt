@file:Suppress("Annotator")

package com.kratsapps.memedom.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.kratsapps.memedom.fragments.HomeFragment
import com.kratsapps.memedom.fragments.MessagesFragment
import com.kratsapps.memedom.fragments.SettingsFragment
import com.kratsapps.memedom.fragments.ProfileFragment

data class Fragments(val fragment: Fragment)

class PageAdapter(val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = arrayListOf<Fragments>()

    init {
        fragments.add(Fragments(ProfileFragment()))
        fragments.add(Fragments(HomeFragment()))
        fragments.add(Fragments(SettingsFragment()))
        fragments.add(Fragments(MessagesFragment()))
    }

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position].fragment

}