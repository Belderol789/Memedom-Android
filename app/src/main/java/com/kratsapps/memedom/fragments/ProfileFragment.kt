package com.kratsapps.memedom.fragments

import DefaultItemDecorator
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.FeedAdapter
import com.kratsapps.memedom.ImageAdapter
import com.kratsapps.memedom.R
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler


class ProfileFragment : Fragment() {

    lateinit var profileContext: Context
    lateinit var profileView: CardView
    lateinit var profileRecyclerView: RecyclerView

    lateinit var galleryAdapter: ImageAdapter
    lateinit var galleryRecyclerView: RecyclerView
    var profileIsExpanded: Boolean = false

    var images: List<String> = listOf()

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
        galleryRecyclerView = rootView.findViewById(R.id.galleryRecycler) as RecyclerView
        getAllUserMemes()
        setupUI(rootView)
        setupGallery()
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
            params.topMargin = height + 800
            profileView.requestLayout()
        }

        val expandBtn = rootView.findViewById(R.id.expandBtn) as ImageButton
        expandBtn.setOnClickListener {
            if (!profileIsExpanded) {
                profileView
                    .animate()
                    .setDuration(500)
                    .translationY((-(height) + 800).toFloat())
                    .withEndAction {
                        profileIsExpanded = !profileIsExpanded
                    }

                expandBtn
                    .animate()
                    .setDuration(500)
                    .rotationBy(180f)
                    .start()
            } else {
                profileView
                    .animate()
                    .setDuration(500)
                    .translationY(-0F)
                    .withEndAction {
                        profileIsExpanded = !profileIsExpanded
                    }
                expandBtn
                    .animate()
                    .setDuration(500)
                    .rotationBy(180f)
                    .start()

            }
        }

        val editBtn = rootView.findViewById<Button>(R.id.editButton)
        editBtn.setOnClickListener {
            
        }
    }

    private fun setupGallery() {
        val context = this.context
        val activity = this.activity
        val testImageURL = "https://firebasestorage.googleapis.com/v0/b/memedom-fb37b.appspot.com/o/ProfilePhotos%2FEO8ndAd7DCaWj63BxrVkIaVjijz1?alt=media&token=a3440239-acc7-4570-8fe9-97d51af46ec7"
        val images = listOf<String>(testImageURL, testImageURL, testImageURL, testImageURL, testImageURL, testImageURL, testImageURL, testImageURL, testImageURL, testImageURL, testImageURL)
            //DatabaseManager(profileContext).retrieveSavedUser()?.gallery
        if (context != null && activity != null && images != null) {
            galleryAdapter = ImageAdapter(images, activity)
            galleryRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
            galleryRecyclerView.adapter = galleryAdapter
            galleryRecyclerView.layoutManager = LinearLayoutManager(activity)
            galleryRecyclerView.itemAnimator?.removeDuration
        }

        val manager = GridLayoutManager(this.context!!,9, GridLayoutManager.VERTICAL, false)
        manager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // 7 is the sum of items in one repeated section
                return 3
                throw IllegalStateException("internal error")
            }
        }
        galleryRecyclerView.setLayoutManager(manager)

    }

    fun Activity.displayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }
}