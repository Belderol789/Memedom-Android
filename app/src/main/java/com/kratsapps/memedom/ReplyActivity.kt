package com.kratsapps.memedom

import DefaultItemDecorator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.facebook.internal.Utility
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_comments.*
import kotlinx.android.synthetic.main.activity_reply.*

class ReplyActivity : AppCompatActivity() {

    lateinit var commentReply: Comments
    var replies: List<Comments> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reply)

        commentReply = intent.extras?.get("CommentReply") as Comments
        setupUI()
        setupAction()
        FirestoreHandler().getAllReplies(commentReply) {
            replies = it
            setupRecyclerView()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun setupUI() {
        sendReplyButton.isEnabled = false
        replyUsername.text = commentReply.userName
        Glide.with(this)
            .load(commentReply.userPhotoURL)
            .centerCrop()
            .into(profileButton)
        replyDate.text = commentReply.commentDateString()
        replysTextView.text = commentReply.commentText
        repliesBtn.setText("${commentReply.commentRepliesCount} Replies")
        upvoteBtn.setText("   ${commentReply.getCommentLikeCount()}")

        replyBackButton.setOnClickListener {
            onBackPressed()
        }

        val mainUserUID = DatabaseManager(this).getMainUserID()
        if (commentReply.commentLikers.contains(mainUserUID) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            upvoteBtn.setTextColor(Color.parseColor("#FACE0D"))
            upvoteBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#FACE0D")))
        }

    }

    private fun setupAction() {
        replyTextMultiLine.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > 0) {
                    sendReplyButton.isEnabled = true
                    sendReplyButton.drawable.setColorFilter(
                        Color.parseColor("#FACE0D"),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    replyTextMultiLine.getBackground().setTint(Color.parseColor("#FACE0D"))
                } else {
                    deactivateSendButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        sendReplyButton.setOnClickListener {

            val mainUserUID = DatabaseManager(this).getMainUserID()
            val commentText = replyTextMultiLine.text.toString()
            val today = System.currentTimeMillis()

            if (!commentText.isEmpty() && mainUserUID != null && commentReply.commentID != null) {

                Log.d("Comment", "Comment text $commentText")

                val commentHash: HashMap<String, Any> = hashMapOf(
                    "commentID" to commentReply.commentID,
                    "commentText" to commentText,
                    "postID" to commentReply.postID,
                    "userName" to commentReply.userName,
                    "userPhotoURL" to commentReply.userPhotoURL,
                    "commentDate" to today,
                    "commentLikers" to arrayListOf<String>(mainUserUID),
                    "commentRepliesCount" to 0,
                    "isComments" to false
                )

                val newComment = Comments()
                newComment.commentID = commentReply.commentID
                newComment.commentText = commentText
                newComment.postID = commentReply.postID
                newComment.userName = commentReply.userName
                newComment.userPhotoURL = commentReply.userPhotoURL
                newComment.commentDate = today
                newComment.commentLikers = arrayListOf<String>(mainUserUID)
                newComment.commentRepliesCount = 0
                newComment.isComments = false

                Log.d("Comments", "Sorted Comments ${replies.count()}")

                replies = listOf(newComment) + replies
                repliesBtn.setText("${replies.count()} Replies")

                Log.d("Comments", "Sorted Comments ${replies.count()} Adapter ${replyRecyclerView.adapter}")

                setupRecyclerView()
                replyTextMultiLine.setText(null)

                hideKeyboard()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    deactivateSendButton()
                }

                val replyID = Utility.generateRandomString(10)
                Log.d("Comment", "Sending comment hash $commentHash")
                FirestoreHandler().sendUserReplyToFirestore(commentReply, replyID, replies.count(), commentHash)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun deactivateSendButton() {
        sendReplyButton.isEnabled = false
        sendReplyButton.drawable.setColorFilter(
            Color.parseColor("#C0C0C0"),
            PorterDuff.Mode.SRC_ATOP
        )
        replyTextMultiLine.getBackground().setTint(Color.parseColor("#C0C0C0"))
    }

    private fun setupRecyclerView() {
        Log.d("Memes", "recyclerview setup")
        val context = applicationContext
        val activity = this
        if (activity != null) {
            val commentsAdapter = CommentsAdapter(replies, activity)

            replyRecyclerView.addItemDecoration(DefaultItemDecorator(resources.getDimensionPixelSize(R.dimen.vertical_recyclerView)))
            replyRecyclerView.adapter = commentsAdapter
            replyRecyclerView.layoutManager = LinearLayoutManager(activity)
            replyRecyclerView.setHasFixedSize(true)
            replyRecyclerView.itemAnimator?.removeDuration
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