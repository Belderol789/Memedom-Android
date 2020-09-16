@file:Suppress("Annotator")

package com.kratsapps.memedom

import android.content.Context
import android.provider.Settings.Global.getString
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.annotation.NonNull
import androidx.annotation.StringRes

data class Fragments(val fragment: Fragment)

class PageAdapter(val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = arrayListOf<Fragments>()

    init {
        fragments.add(Fragments(Profile()))
        fragments.add(Fragments(Home()))
        fragments.add(Fragments(Notifications()))
        fragments.add(Fragments(Messages()))
    }

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position].fragment

}