package com.kratsapps.memedom

import DefaultItemDecorator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.facebook.internal.Utility.generateRandomString
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_comments.*


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

            val shareCount = if(postMeme.postShares >= 10) "${postMeme.postShares}" else ""
            commentsShareBtn.text = shareCount

            val commentsCount = if(postMeme.postComments >= 10) "${postMeme.postComments}"  else ""
            commentsCommentsBtn.text = commentsCount

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
                .circleCrop()
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
                    "commentRepliesCount" to 0,
                    "showActions" to true
                )

                val newComment = Comments()
                newComment.commentID = commentID
                newComment.commentText = commentText
                newComment.postID = postMeme.postID
                newComment.userName = postMeme.postUsername
                newComment.userPhotoURL = postMeme.postProfileURL
                newComment.commentDate = today
                newComment.commentLikers = arrayListOf<String>(mainUserUID)
                newComment.commentRepliesCount = 0
                newComment.showActions = true

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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

}