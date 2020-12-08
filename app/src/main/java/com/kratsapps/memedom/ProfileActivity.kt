package com.kratsapps.memedom

import DefaultItemDecorator
import android.app.Activity
import android.content.Intent
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.adapters.FeedAdapter
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.adapters.ImageAdapter
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    var galleryItems: MutableList<String> = mutableListOf()

    var matchUser: Matches? = null
    var memeDomUser: MemeDomUser? = null
    var matchUserID = ""
    var matchUserMemes = mutableListOf<Memes>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        congratsView.visibility = View.GONE
        matchUser = intent.extras?.get("MatchUser") as? Matches
        setupUI()
    }

    private fun setupUI() {
        profileActivityLoadingView.visibility = View.VISIBLE
        Glide.with(this)
            .asGif()
            .load(R.raw.loader)
            .into(loadingImageView)

        FirestoreHandler().getUsersDataWith(matchUserID, {
            memeDomUser = it
            matchUserID = it.email + it.uid
            setupUserData()
            setupGallery()
            getAllUserMemes()
            setupActionButtons(it)
            profileActivityLoadingView.visibility = View.GONE
        })
    }

    private fun setupActionButtons(memeDomUser: MemeDomUser) {
        rejectBtn.setOnClickListener {
            FirestoreHandler().rejectUser(memeDomUser, this)
            onBackPressed()
        }

        matchBtn.setOnClickListener {
            if (matchUser != null) {
                val data = hashMapOf<String, Any>(
                    "matchStatus" to true,
                    "matchDate" to System.currentTimeMillis()
                )
                FirestoreHandler().updateMatch(matchUserID, data, this, {})
                FirestoreHandler().updateUserLiked(matchUserID, this, {
                    val intent = Intent().apply {
                        putExtra("ChatMatch", matchUser)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    onBackPressed()
                })

            } else {
                congratsView.visibility = View.VISIBLE
            }

        }

        okBtn.setOnClickListener {
            FirestoreHandler().sendToMatchUser(memeDomUser!!, this)
            onBackPressed()
        }
    }

    private fun setupUserData() {
        congratsText.setText("Congrats on connecting! You'll be able to chat with ${memeDomUser!!.name} if they accept your invitation")
        username.setText(memeDomUser!!.name)
        gender.setText(memeDomUser!!.gender)

        if(memeDomUser!!.bio.isBlank()) {
            bioText.setText("No Bio Available")
        } else {
            bioText.setText(memeDomUser!!.bio)
        }

        Glide.with(this)
            .load(memeDomUser!!.profilePhoto)
            .circleCrop()
            .into(profilePhoto)
    }

    private fun setupGallery() {

        var images = memeDomUser!!.gallery
        val galleryManager: GridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)

        if (images != null) {
            Log.d("UserGallery", "User photos $images")
            galleryItems = images.toMutableList()
            val galleryAdapter = ImageAdapter(galleryItems, null, this, null, false)
            galleryRecycler.adapter = galleryAdapter
            galleryRecycler.layoutManager = galleryManager
            galleryRecycler.itemAnimator?.removeDuration
        }
    }

    private fun getAllUserMemes() {
        matchUserMemes.clear()
        FirestoreHandler().getAllMemesOfMainUser(memeDomUser!!.uid) {
            matchUserMemes.add(it)
            var crown: Int = 0
            for (meme in matchUserMemes) {
                crown += meme.getPostLikeCount()
            }

            matchCount.setText("${memeDomUser!!.matches.count()}")
            postCount.setText("${matchUserMemes.count()}")
            crownsCount.setText("$crown")

            var userMemes = mutableListOf<String>()
            for (meme in matchUserMemes) {
                userMemes.add(meme.postImageURL)
            }

            val feedAdapter = ImageAdapter(userMemes, matchUserMemes, this, null, true)
            val galleryManager: GridLayoutManager =
                GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
            profileRecycler.adapter = feedAdapter
            profileRecycler.layoutManager = galleryManager
            profileRecycler.itemAnimator?.removeDuration
        }
    }
}