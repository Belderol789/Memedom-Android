package com.kratsapps.memedom.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.Assets
import com.kratsapps.memedom.R
import com.kratsapps.memedom.ReplyActivity
import com.kratsapps.memedom.models.Comments
import kotlinx.android.synthetic.main.comments_item.view.*


class CommentsAdapter(private val commentList: List<Comments>, private val activity: Activity): RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    lateinit var commentAdapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.comments_item,
            parent, false
        )
        commentAdapterContext = parent.context
        return CommentViewHolder(
            itemView
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val currentComment = commentList[position]
        // User info
        Glide.with(commentAdapterContext)
            .load(currentComment.userPhotoURL)
            .circleCrop()
            .into(holder.userPhotoURL)
        holder.userName.setText(currentComment.userName)
        holder.commentDate.setText(currentComment.commentDateString())
        holder.commentText.setText(currentComment.commentText)

        Log.d("Replies","Reply ${currentComment.commentID} isComments ${currentComment.showActions}")

        if (currentComment.showActions) {
            holder.commentActionLayout.visibility = View.VISIBLE
        } else {
            holder.commentActionLayout.visibility = View.GONE
        }

        if (currentComment.commentRepliesCount > 0) {
            holder.repliesBtn.setText("View ${currentComment.commentRepliesCount} Replies")
        } else {
            holder.repliesBtn.setText("Reply")
        }
        holder.upvoteBtn.setText("   ${currentComment.getCommentLikeCount()}")

        val mainUserID = DatabaseManager(commentAdapterContext).getMainUserID()

        if(mainUserID != null) {

            if (currentComment.commentLikers.contains(mainUserID)) {
                setLikeBtn(holder, true)
            } else {
                setLikeBtn(holder, false)
            }

            holder.upvoteBtn.setOnClickListener {
                updateLikers(mainUserID, currentComment, holder)
            }
        }

        holder.repliesBtn.setOnClickListener {
            navigateToReplies(currentComment)
        }

    }

    private fun updateLikers(mainUserID: String, currentComment: Comments, holder: CommentViewHolder) {
        if(currentComment.commentLikers.contains(mainUserID)) {
            val updatedLikes = currentComment.getCommentLikeCount() - 1
            holder.upvoteBtn.setText("   $updatedLikes")
            setLikeBtn(holder, false)

            currentComment.commentLikers -= mainUserID
            FirestoreHandler().removeCommentPoints(mainUserID, currentComment.postID, currentComment.commentID)
        } else {
            val updatedLikes = currentComment.getCommentLikeCount() + 1
            holder.upvoteBtn.setText("   $updatedLikes")
            setLikeBtn(holder, true)

            currentComment.commentLikers += mainUserID
            FirestoreHandler().updateCommentPoints(mainUserID, currentComment.postID, currentComment.commentID)
        }
    }

    private fun setLikeBtn(holder: CommentViewHolder, active: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (active) {
                holder.upvoteBtn.setTextColor(Assets().appFGColor)
                holder.upvoteBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Assets().appFGColor))
            } else {
                holder.upvoteBtn.setTextColor(Color.parseColor("#C0C0C0"))
                holder.upvoteBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#C0C0C0")))
            }
        }
    }

    override fun getItemCount() = commentList.size

    class CommentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val userName = itemView.commentUsername
        val commentDate = itemView.commentDate
        val userPhotoURL = itemView.profileButton

        val commentText = itemView.commentsTextView
        val repliesBtn = itemView.repliesBtn
        val upvoteBtn = itemView.upvoteBtn

        val commentActionLayout = itemView.commentActionLayout
    }

    private fun navigateToReplies(comment: Comments) {
        val intent: Intent = Intent(commentAdapterContext, ReplyActivity::class.java)
        intent.putExtra("CommentReply", comment)
        commentAdapterContext.startActivity(intent)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }
}



