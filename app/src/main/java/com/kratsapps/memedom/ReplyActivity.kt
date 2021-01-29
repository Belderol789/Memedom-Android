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
import com.kratsapps.memedom.adapters.CommentsAdapter
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.utils.hideKeyboard
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
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun setupUI() {

        for (reply in commentReply.replies) {
            if(reply is HashMap<*,*>) {

                Log.d("CheckReplies", "Reply Data $reply")

                val newReply = Comments()
                newReply.replies = listOf()
                newReply.userPhotoURL = reply.get("userPhotoURL") as String
                newReply.commentText = reply.get("commentText") as String
                newReply.commentDate = reply.get("commentDate") as Long
                newReply.commentID = reply.get("commentID") as String
                newReply.postID = reply.get("postID") as String
                newReply.userName = reply.get("userName") as String
                newReply.showActions = reply.get("showActions") as Boolean
                newReply.commentLikers = reply.get("commentLikers") as List<String>

                replies += newReply
            } else {
                Log.d("CheckReplies", "Wrong replies")
            }
        }


        Log.d("CheckReplies", "Current replies $replies")

        sendReplyButton.isEnabled = false
        replyUsername.text = commentReply.userName
        Glide.with(this)
            .load(commentReply.userPhotoURL)
            .circleCrop()
            .into(profileButton)
        replyDate.text = commentReply.commentDateString()
        replysTextView.text = commentReply.commentText
        repliesBtn.setText("${commentReply.replies.count()} Replies")
        upvoteBtn.setText("   ${commentReply.getCommentLikeCount()}")

        replyBackButton.setOnClickListener {
            onBackPressed()
        }

        val mainUserUID = DatabaseManager(this).retrieveSavedUser()
        if (mainUserUID != null) {
            if (commentReply.commentLikers.contains(mainUserUID) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                upvoteBtn.setTextColor(Color.parseColor("#FACE0D"))
                upvoteBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#FACE0D")))
            }
        }

        setupRecyclerView()

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

            val mainUser = DatabaseManager(this).retrieveSavedUser()
            val commentText = replyTextMultiLine.text.toString()
            val today = System.currentTimeMillis()

            if (!commentText.isEmpty() && mainUser != null && commentReply.commentID != null) {

                Log.d("Comment", "Comment text $commentText")

                val newComment = Comments()
                newComment.commentID = commentReply.commentID
                newComment.commentText = commentText
                newComment.postID = commentReply.postID
                newComment.userName = mainUser.name
                newComment.userPhotoURL = mainUser.profilePhoto
                newComment.commentDate = today
                newComment.commentLikers = arrayListOf<String>(mainUser.uid)
                newComment.replies = listOf()
                newComment.showActions = false

                val commentHash: HashMap<String, Any> = hashMapOf(
                    "commentID" to newComment.commentID,
                    "commentText" to newComment.commentText,
                    "postID" to newComment.postID,
                    "userName" to newComment.userName,
                    "userPhotoURL" to newComment.userPhotoURL,
                    "commentDate" to newComment.commentDate,
                    "commentLikers" to newComment.commentLikers,
                    "showActions" to false
                )

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
                FirestoreHandler().sendUserReplyToFirestore(commentReply, commentHash)
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
        val context = applicationContext
        val activity = this
        if (activity != null) {
            Log.d("Memes", "recyclerview setup with $replies")

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