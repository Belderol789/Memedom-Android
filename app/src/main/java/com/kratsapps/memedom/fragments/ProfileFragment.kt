package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.app.Activity
import android.content.Context
import android.media.Image
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.FeedAdapter
import com.kratsapps.memedom.R
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.fragment_create.*
import kotlinx.android.synthetic.main.fragment_profile.*
import org.w3c.dom.Text


class ProfileFragment : Fragment() {

    lateinit var profileContext: Context
    lateinit var profileView: CardView
    lateinit var profileRecyclerView: RecyclerView
    var profileIsExpanded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        profileContext = context
        Log.d("OnCreateView", "Called Attached")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)
        profileRecyclerView = rootView.findViewById(R.id.profileRecycler)
        getAllUserMemes()
        setupUI(rootView)
        return rootView
    }

    private fun getAllUserMemes() {
        val mainUserID = DatabaseManager(profileContext).getMainUserID()
        if(mainUserID != null && this.activity != null) {
            FirestoreHandler().getAllMemesOfMainUser(mainUserID) {
                val feedAdapter = FeedAdapter(it, this.activity!!)
                profileRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
                profileRecyclerView.adapter = feedAdapter
                profileRecyclerView.layoutManager = LinearLayoutManager(activity)
                profileRecyclerView.setHasFixedSize(true)
                profileRecyclerView.itemAnimator?.removeDuration
            }
        }
    }

    private fun setupUI(rootView: View) {

        profileView = rootView.findViewById<CardView>(R.id.profile_cardView)
        val username = rootView.findViewById<TextView>(R.id.username)
        val gender = rootView.findViewById<TextView>(R.id.gender)
        val profilePhoto = rootView.findViewById<ImageButton>(R.id.profilePhoto)

        val mainUser = DatabaseManager(profileContext).retrieveSavedUser()
        if (mainUser != null) {
            username.setText(mainUser.name)
            gender.setText(mainUser.gender)
            Glide.with(this.activity!!)
                .load(mainUser.profilePhoto)
                .circleCrop()
                .into(profilePhoto)
        }

        var height: Int = 0
        var width: Int = 0

        activity?.displayMetrics()?.run {
            height = heightPixels
            width = widthPixels

            val params = profileView.layoutParams as ConstraintLayout.LayoutParams
            params.height = height
            params.width = width
            params.topMargin = height + 950
            profileView.requestLayout()
        }

        val expandBtn = rootView.findViewById(R.id.expandBtn) as ImageButton
        expandBtn.setOnClickListener {
            if (!profileIsExpanded) {
                profileView
                    .animate()
                    .setDuration(500)
                    .translationY((-(height) + 950).toFloat())
                    .withEndAction {
                        expandBtn.setImageResource(R.drawable.ic_action_down_arrow)
                        profileIsExpanded = !profileIsExpanded
                    }
            } else {
                profileView
                    .animate()
                    .setDuration(500)
                    .translationY(-0F)
                    .withEndAction {
                        expandBtn.setImageResource(R.drawable.ic_action_up_arrow)
                        profileIsExpanded = !profileIsExpanded
                    }

            }
        }
    }

    fun Activity.displayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }
}