package com.kratsapps.memedom

import DefaultItemDecorator
import android.R.color
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.internal.Utility.generateRandomString
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_comments.*
import kotlinx.android.synthetic.main.activity_credential.view.*
import kotlinx.android.synthetic.main.comments_item.*
import org.w3c.dom.Comment


class CommentsActivity : AppCompatActivity() {

    lateinit var postMeme: Memes
    var comments: List<Comments> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)
        postMeme = intent.extras?.get("CommentMeme") as Memes
        setupUI()
        setupActionUI()
        FirestoreHandler().checkForComments(postMeme.postID, {
            comments = it
            comments.sortedBy { it.commentDate }
            Log.d("Comments", "Got new comments $comments")
            //setup recycler view
            setupFeedView()
        })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun setupUI() {
        commentBackButton.setOnClickListener {
            onBackPressed()
        }

        if (postMeme != null) {
            commentsTitle.text = postMeme.postTitle
            commentsDate.text = postMeme.postDateString()
            commentsPointsTextView.text = "${postMeme.getPostLikeCount()}"
            commentsCommentsBtn.text = "${postMeme.postComments}"
            commentsShareBtn.text = "${postMeme.postShares}"

            val mainUserID = DatabaseManager(this).getMainUserID()
            if (mainUserID != null && postMeme.postLikers.contains(mainUserID)) {
                commentsPointsLayout.visibility = View.VISIBLE
                commentsUserInfo.visibility = View.VISIBLE
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val appFGColor = Color.parseColor("#FACE0D")
                    commentsCommentsBtn.setTextColor(appFGColor)
                    commentsShareBtn.setTextColor(appFGColor)
                    commentsCommentsBtn.setCompoundDrawableTintList(ColorStateList.valueOf(appFGColor))
                    commentsShareBtn.setCompoundDrawableTintList(ColorStateList.valueOf(appFGColor))
                }
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun deactivateSendButton() {
        sendButton.isEnabled = false
        sendButton.drawable.setColorFilter(
            Color.parseColor("#C0C0C0"),
            PorterDuff.Mode.SRC_ATOP
        )
        editTextTextMultiLine.getBackground().setTint(Color.parseColor("#C0C0C0"))
    }

    private fun setupActionUI() {
        editTextTextMultiLine.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > 0) {
                    sendButton.isEnabled = true
                    sendButton.drawable.setColorFilter(
                        Color.parseColor("#FACE0D"),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    editTextTextMultiLine.getBackground().setTint(Color.parseColor("#FACE0D"))
                } else {
                    deactivateSendButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        sendButton.setOnClickListener {

            val mainUserUID = DatabaseManager(this).getMainUserID()
            val commentText = editTextTextMultiLine.text.toString()
            val today = System.currentTimeMillis()

            if (!commentText.isEmpty() && mainUserUID != null && postMeme.postID != null) {

                Log.d("Comment", "Comment text $commentText")

                val commentID = generateRandomString(10)

                val commentHash: HashMap<String, Any> = hashMapOf(
                    "commentID" to commentID,
                    "commentText" to commentText,
                    "postID" to postMeme.postID,
                    "userName" to postMeme.postUsername,
                    "userPhotoURL" to postMeme.postProfileURL,
                    "commentDate" to today,
                    "commentLikers" to arrayListOf<String>(mainUserUID),
                    "commentReplies" to arrayListOf<String>()
                )

                val newComment = Comments()
                newComment.commentID = commentID
                newComment.commentText = commentText
                newComment.postID = postMeme.postID
                newComment.userName = postMeme.postUsername
                newComment.userPhotoURL = postMeme.postProfileURL
                newComment.commentDate = today
                newComment.commentLikers = arrayListOf<String>(mainUserUID)
                newComment.commentReplies = arrayListOf<String>()

                Log.d("Comments", "Sorted Comments ${comments.count()}")

                comments = listOf(newComment) + comments

                Log.d("Comments", "Sorted Comments ${comments.count()} Adapter ${commentsRecyclerView.adapter}")

                setupFeedView()
                editTextTextMultiLine.setText(null)

                hideKeyboard()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    deactivateSendButton()
                }

                Log.d("Comment", "Sending comment hash $commentHash")
                FirestoreHandler().sendUserCommentToFirestore(postMeme.postID, commentID, comments.count(), commentHash)
            }
        }
    }

    private fun setupFeedView() {
        Log.d("Memes", "recyclerview setup")
        val context = applicationContext
        val activity = this
        if (activity != null) {
            val commentsAdapter = CommentsAdapter(comments, activity)

            commentsRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
            commentsRecyclerView.adapter = commentsAdapter
            commentsRecyclerView.layoutManager = LinearLayoutManager(activity)
            commentsRecyclerView.setHasFixedSize(true)
            commentsRecyclerView.itemAnimator?.removeDuration
        }
    }
}