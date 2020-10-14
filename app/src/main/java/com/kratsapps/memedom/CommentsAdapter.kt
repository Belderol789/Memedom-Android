package com.kratsapps.memedom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.DoubleClickListener
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.activity_comments.*
import kotlinx.android.synthetic.main.activity_comments.view.*
import kotlinx.android.synthetic.main.comments_item.view.*
import kotlinx.android.synthetic.main.feed_item.view.*
import org.w3c.dom.Comment


class CommentsAdapter(private val commentList: List<Comments>, private val activity: Activity): RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    lateinit var commentAdapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.comments_item,
            parent, false
        )
        commentAdapterContext = parent.context
        return CommentViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val currentComment = commentList[position]
        // User info
        Glide.with(commentAdapterContext)
            .load(currentComment.userPhotoURL)
            .centerCrop()
            .into(holder.userPhotoURL)
        holder.userName.setText(currentComment.userName)
        holder.commentDate.setText(currentComment.commentDateString())

        holder.commentText.setText(currentComment.commentText)

        if (currentComment.getCommentReplyCount() > 0) {
            holder.repliesBtn.setText("View ${currentComment.getCommentReplyCount()} Replies")
        } else {
            holder.repliesBtn.setText("Reply")
        }
        holder.upvoteBtn.setText("   ${currentComment.getCommentLikeCount()}")


        val mainUserID = DatabaseManager(commentAdapterContext).getMainUserID()

        if(mainUserID != null) {
            holder.upvoteBtn.setOnClickListener {
                updateLikers(mainUserID, currentComment, holder)
            }
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setLikeBtn(holder: CommentViewHolder, active: Boolean) {
        if (active) {
            holder.upvoteBtn.setTextColor(Color.parseColor("#FACE0D"))
            holder.upvoteBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#FACE0D")))
        } else {
            holder.upvoteBtn.setTextColor(Color.parseColor("#C0C0C0"))
            holder.upvoteBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#C0C0C0")))
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
    }
}


