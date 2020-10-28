package com.kratsapps.memedom

import DefaultItemDecorator
import android.app.Activity
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.fragments.ProfileFragment
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FeedAdapter
import com.kratsapps.memedom.utils.FirestoreHandler
import com.kratsapps.memedom.utils.ImageAdapter
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    var galleryItems: MutableList<String> = mutableListOf()
    var profileIsExpanded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        congratsView.visibility = View.GONE

        val matchedUser = intent.extras?.get("MatchedUser") as? MemeDomUser


        if (matchedUser != null) {
            setupUserData(matchedUser)
            getAllUserMemes(matchedUser.uid)
            congratsText.setText("Congrats on connecting! You'll be able to chat with ${matchedUser.name} if they accept your invitation")

            rejectBtn.setOnClickListener {
                FirestoreHandler().rejectUser(matchedUser, this)
                onBackPressed()
            }

            matchBtn.setOnClickListener {

                congratsView.visibility = View.VISIBLE
            }

            okBtn.setOnClickListener {
                FirestoreHandler().matchUser(matchedUser, this)
                onBackPressed()
            }
        }

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        var width = size.x
        var height = size.y

        val params = profile_cardView.layoutParams as ConstraintLayout.LayoutParams
        params.height = height
        params.width = width
        params.topMargin = height + 800
        profile_cardView.requestLayout()


        expandBtn.setOnClickListener {
            if (!profileIsExpanded) {
                profile_cardView
                    .animate()
                    .setDuration(500)
                    .translationY((800 - height).toFloat())
                    .withEndAction {
                        profileIsExpanded = !profileIsExpanded
                    }

                expandBtn
                    .animate()
                    .setDuration(500)
                    .rotationBy(180f)
                    .start()
            } else {
                profile_cardView
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
    }

    private fun setupUserData(matchedUser: MemeDomUser) {
        username.setText(matchedUser.name)
        gender.setText(matchedUser.gender)

        if(matchedUser.bio.isBlank()) {
            bioText.setText("No Bio Available")
        } else {
            bioText.setText(matchedUser.bio)
        }

        Glide.with(this)
            .load(matchedUser.profilePhoto)
            .circleCrop()
            .into(profilePhoto)

        setupGallery(matchedUser.gallery)
    }

    private fun setupGallery(images: List<String>) {
        val galleryManager: GridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        galleryItems = images.toMutableList()
        val galleryAdapter = ImageAdapter(galleryItems, this, null)
        galleryRecycler.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
        galleryRecycler.adapter = galleryAdapter
        galleryRecycler.layoutManager = galleryManager
        galleryRecycler.itemAnimator?.removeDuration
    }

    private fun getAllUserMemes(mainUserID: String) {
        FirestoreHandler().getAllMemesOfMainUser(mainUserID) {
            val feedAdapter =  FeedAdapter(it, this)
            profileRecycler.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
            profileRecycler.adapter = feedAdapter
            profileRecycler.layoutManager = LinearLayoutManager(this)
            profileRecycler.setHasFixedSize(true)
            profileRecycler.itemAnimator?.removeDuration
        }
    }

}