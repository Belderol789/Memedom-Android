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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.adapters.FeedAdapter
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.adapters.ImageAdapter
import com.kratsapps.memedom.models.Matches
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    var galleryItems: MutableList<String> = mutableListOf()
    var profileIsExpanded: Boolean = false

    var matchUser: Matches? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        congratsView.visibility = View.GONE
        matchUser = intent.extras?.get("MatchUser") as? Matches

        var matchUserID = ""
        if (matchUser?.uid != null) {
            matchUserID = matchUser!!.uid
        } else {
            matchUserID = intent.extras?.getString("MatchID") as String
        }

        if (!matchUserID.isEmpty()) {
            getAllUserMemes(matchUserID)

            FirestoreHandler().getUserDataWith(matchUserID, {

                val memeDomUser = it
                setupUserData(it)

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
                    FirestoreHandler().sendToMatchUser(memeDomUser, this)
                    onBackPressed()
                }
            })
        }

        var display: Display?

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = this.display
        } else {
            display = windowManager.defaultDisplay
        }
        val size = Point()
        display?.getRealSize(size)
        var width = size.x
        var height = size.y

        Log.d("heightIs", "$height")

        val params = profile_cardView.layoutParams as ConstraintLayout.LayoutParams
        params.height = height
        params.width = width
        params.topMargin = height
        profile_cardView.requestLayout()

        expandBtn.setOnClickListener {
            if (!profileIsExpanded) {
                profile_cardView
                    .animate()
                    .setDuration(500)
                    .translationY(((height / 1.8) - height).toFloat())
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
        congratsText.setText("Congrats on connecting! You'll be able to chat with ${matchedUser.name} if they accept your invitation")
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
        val galleryAdapter = ImageAdapter(galleryItems, null,this, null, false)
        galleryRecycler.adapter = galleryAdapter
        galleryRecycler.layoutManager = galleryManager
        galleryRecycler.itemAnimator?.removeDuration
    }

    private fun getAllUserMemes(mainUserID: String) {
//        FirestoreHandler().getAllMemesOfMainUser(mainUserID) {
//            val feedAdapter =  FeedAdapter(it, this, true)
//            profileRecycler.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
//            profileRecycler.adapter = feedAdapter
//            profileRecycler.layoutManager = LinearLayoutManager(this)
//            profileRecycler.setHasFixedSize(true)
//            profileRecycler.itemAnimator?.removeDuration
//        }
    }

}