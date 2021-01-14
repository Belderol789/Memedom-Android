package com.kratsapps.memedom

import DefaultItemDecorator
import android.content.Context
import android.content.Intent
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.facebook.internal.Utility.generateRandomString
import com.google.android.gms.ads.AdRequest
import com.google.firebase.firestore.FieldValue
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.adapters.CommentsAdapter
import com.kratsapps.memedom.adapters.FeedAdapter
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.utils.hideKeyboard
import kotlinx.android.synthetic.main.activity_comments.*
import kotlinx.android.synthetic.main.fragment_create.*


class CommentsActivity : AppCompatActivity() {

    lateinit var postMeme: Memes
    var isMemeDom: Boolean = true
    var comments: List<Comments> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)
        postMeme = intent.extras?.get("CommentMeme") as Memes
        isMemeDom = intent.extras!!.getBoolean("isMemeDom")
        setupUI()
        setupActionUI()
        FirestoreHandler().checkForComments(postMeme.postID, {
            comments = it
            comments.sortedBy { it.commentDate }
            Log.d("Comments", "Got new comments $comments")
            setupFeedView()
        })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.exit_activity, R.anim.exit_activity)
    }

    private fun navigateToLargeImage(imageURI: String) {
        val intent: Intent = Intent(this, ImageActivity::class.java)
        intent.putExtra("EnlargeImageURL", imageURI)
        this.startActivity(intent)
    }

    private fun setupUI() {
        commentBackButton.setOnClickListener {
            onBackPressed()
        }

        commentsImage.setOnClickListener {
            navigateToLargeImage(postMeme.postImageURL)
        }

        val mainUser = DatabaseManager(this).retrieveSavedUser()

        if (postMeme != null) {
            commentsTitle.text = postMeme.postTitle
            commentsDate.text = postMeme.postDateString()

            commentsUsername.text = postMeme.postUsername
            val mainUser = DatabaseManager(this).retrieveSavedUser()
            val mainUserID = mainUser?.uid

            if (mainUserID != null && postMeme.postLikers.contains(mainUserID)) {
                commentsUserInfo.visibility = View.VISIBLE
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    var itemColor = Color.parseColor("#8FD6EF")
                    if(mainUser.matches.contains(postMeme.postUserUID)) {
                        itemColor = Color.parseColor("#FACE0D")
                    }
                }
            } else {
                commentsUserInfo.visibility = View.GONE
            }

            Glide.with(this)
                .load(postMeme.postImageURL)
                .fitCenter()
                .into(commentsImage)
            Glide.with(this)
                .load(postMeme.postProfileURL)
                .circleCrop()
                .into(commentsProfilePic)

            commentShareBtn.text = "${postMeme.postShares}"
            commentLikeBtn.text = "${postMeme.getPostLikeCount()}"
            commentCommentsBtn.text = "${postMeme.postComments}"
        }

        val adRequest = AdRequest.Builder().build()
        commentAdView.visibility = View.VISIBLE
        commentAdView.loadAd(adRequest)

        //Memedom
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
             if (isMemeDom) {
                val crownImage = this.baseContext.resources.getDrawable(R.drawable.ic_action_crown, null)
                commentLikeBtn.setCompoundDrawablesWithIntrinsicBounds(crownImage, null, null, null)
                commentLikeImage.setImageResource(R.drawable.ic_action_crown)
                commentLikeImage.setColorFilter(ContextCompat.getColor(this, R.color.appFGColor))
                commentsUserInfo.visibility = View.VISIBLE
            } else {
                val likeImage = this.baseContext.resources.getDrawable(R.drawable.ic_action_like, null)
                commentLikeBtn.setCompoundDrawablesWithIntrinsicBounds(likeImage, null, null, null)
                commentLikeImage.setImageResource(R.drawable.ic_action_like)
                commentLikeImage.setColorFilter(ContextCompat.getColor(this, R.color.appDateFGColor))
                commentsUserInfo.visibility = View.GONE
                if (mainUser != null) {
                    if (mainUser?.matches.contains(postMeme.postUserUID)) {
                        commentsUserInfo.visibility = View.VISIBLE
                    }
                }
            }
        }

        if (mainUser != null) {
            if (postMeme.postLikers.contains(mainUser!!.uid)) {
                if ((mainUser.matches.contains(postMeme.postUserUID))) {
                    alreadyLiked(Color.parseColor("#FACE0D"))
                } else if (isMemeDom) {
                    alreadyLiked(Color.parseColor("#58BADC"))
                } else {
                    alreadyLiked(Color.parseColor("#FF69B4"))
                }
            } else {
                alreadyLiked(Color.parseColor("#C0C0C0"))
            }
        }

        commentLikeBtn.setOnClickListener {
            if(mainUser != null) {

                var fieldValue: FieldValue? = null

                if(!postMeme.postLikers.contains(mainUser.uid)) {
                    Log.d("LikeSystem", "Liking user")
                    //animate in crown
                    fieldValue = FieldValue.arrayUnion(mainUser.uid)
                    postMeme.postLikers += mainUser.uid

                    if ((mainUser.matches.contains(postMeme.postUserUID))) {
                        didLikePost(Color.parseColor("#FACE0D"))
                    } else if (isMemeDom) {
                        didLikePost(Color.parseColor("#58BADC"))
                    } else {
                        didLikePost(Color.parseColor("#FF69B4"))
                    }

                    if (!isMemeDom) {
                        FirestoreHandler().updateLikeDatabase(mainUser.uid, postMeme.postUserUID, this.applicationContext,1, {})
                    }
                    DatabaseManager(this).convertUserObject(mainUser!!, {})
                } else {
                    fieldValue = FieldValue.arrayRemove(mainUser.uid)
                    postMeme.postLikers -= mainUser.uid
                    alreadyLiked(Color.parseColor("#C0C0C0"))
                    Log.d("LikeSystem", "Disliking user")
                }

                val updatedPoints = postMeme.postLikers.count()
                val updatedPointsHash = hashMapOf<String, Any>(
                    "postLikers" to fieldValue!!,
                    "postPoints" to updatedPoints.toLong()
                )

                FirestoreHandler().updateArrayDatabaseObject(
                    "Memes",
                    postMeme.postID,
                    updatedPointsHash
                )
                commentLikeBtn.text = "${postMeme.postLikers.count()}"
            } else {
                Toast.makeText(baseContext, "You must be logged in to like", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun didLikePost(color: Int) {
        commentLikeImage
            .animate()
            .alpha(1.0f)
            .setDuration(750)
            .withEndAction {
                commentLikeImage.animate().alpha(0.0f)
            }
        alreadyLiked(color)
    }

    private fun alreadyLiked(color: Int) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            commentLikeBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            commentShareBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            commentCommentsBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
        }

        commentLikeBtn.setTextColor(color)
        commentShareBtn.setTextColor(color)
        commentCommentsBtn.setTextColor(color)
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
                        Color.parseColor("#8FD6EF"),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    editTextTextMultiLine.getBackground().setTint(Color.parseColor("#8FD6EF"))
                } else {
                    deactivateSendButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        sendButton.setOnClickListener {

            val mainUser = DatabaseManager(this).retrieveSavedUser()
            val commentText = editTextTextMultiLine.text.toString()
            val today = System.currentTimeMillis()

            if (!commentText.isEmpty() && mainUser != null && postMeme.postID != null) {
                val mainUserUID = mainUser.uid
                val commentID = generateRandomString(10)

                val commentHash: HashMap<String, Any> = hashMapOf(
                    "commentID" to commentID,
                    "commentText" to commentText,
                    "postID" to postMeme.postID,
                    "userName" to mainUser.name,
                    "userPhotoURL" to mainUser.profilePhoto,
                    "commentDate" to today,
                    "commentLikers" to arrayListOf<String>(mainUserUID),
                    "commentRepliesCount" to 0,
                    "showActions" to true
                )

                val newComment = Comments()
                newComment.commentID = commentID
                newComment.commentText = commentText
                newComment.postID = postMeme.postID
                newComment.userName = mainUser.name
                newComment.userPhotoURL = mainUser.profilePhoto
                newComment.commentDate = today
                newComment.replies = listOf()
                newComment.commentLikers = arrayListOf<String>(mainUserUID)
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
            } else {
                Toast.makeText(baseContext, "You must be logged in to like", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFeedView() {
        Log.d("Memes", "recyclerview setup")
        val context = applicationContext
        val activity = this
        if (activity != null) {
            val commentsAdapter =
                CommentsAdapter(comments, activity)

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