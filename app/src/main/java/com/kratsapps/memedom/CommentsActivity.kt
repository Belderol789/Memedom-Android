package com.kratsapps.memedom

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import kotlinx.android.synthetic.main.activity_comments.*

class CommentsActivity : AppCompatActivity() {

    lateinit var postMeme: Memes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)
        postMeme = intent.extras?.get("CommentMeme") as Memes
        setupUI()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun setupUI() {
        commentBackButton.setOnClickListener{
            onBackPressed()
        }

        if (postMeme != null) {
            commentsTitle.text = postMeme.postTitle
            commentsDate.text = postMeme.postDateString()
            commentsPointsTextView.text = "${postMeme.getPostLikeCount()}"
            commentsCommentsBtn.text = "${postMeme.postComments}"
            commentsShareBtn.text = "${postMeme.postShares}"

            val mainUserID = DatabaseManager(this).getMainUserID()
            if(mainUserID != null && postMeme.postLikers.contains(mainUserID)) {
                commentsPointsLayout.visibility = View.VISIBLE
                commentsUserInfo.visibility = View.VISIBLE
            } else {
                commentsPointsLayout.visibility = View.GONE
                commentsUserInfo.visibility = View.GONE
            }

            Glide.with(this)
                .load(postMeme.postImageURL)
                .centerCrop()
                .into(commentsImage)
            Glide.with(this)
                .load(postMeme.postProfileURL)
                .centerCrop()
                .into(commentsProfilePic)
        }
    }
}