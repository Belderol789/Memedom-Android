package com.kratsapps.memedom

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
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
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.DoubleClickListener
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.feed_item.view.*


class FeedAdapter(private val feedList: List<Memes>, private val activity: Activity): RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    lateinit var feedAdapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.feed_item,
            parent, false
        )
        feedAdapterContext = parent.context
        return FeedViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val currentItem = feedList[position]
        val postUD: String = currentItem.postID
        val postUserID: String = currentItem.postUserUID
        var currentPostLikes = currentItem.getPostLikeCount()

        Log.d("Scrolling", "Scrolled through meme ${currentItem.postTitle}")

        Glide.with(feedAdapterContext)
            .load(currentItem.postImageURL)
            .centerCrop()
            .into(holder.feedImage)

        holder.postUserName.text = currentItem.postUsername
        holder.likeImageView.alpha = 0f
        holder.shareBtn.text = "${currentItem.postShares}"
        holder.commentsBtn.setOnClickListener {
            navigateToComments(currentItem)
        }
        holder.feedTitle.text = currentItem.postTitle

        Glide.with(feedAdapterContext)
            .load(currentItem.postProfileURL)
            .centerCrop()
            .into(holder.postProfilePic)
        holder.feedDate.text = currentItem.postDateString()

        val mainUser = DatabaseManager(feedAdapterContext).retrieveSavedUser()
        val postLikers = currentItem.postLikers

        holder.feedImage.setOnClickListener(object: DoubleClickListener() {
            override fun onSingleClick(v: View?) {
                Log.d("Gesture", "User has tapped once")
                if(mainUser?.uid != null && postLikers.contains(mainUser.uid)) {
                    navigateToComments(currentItem)
                    //make sure when user logs out, all data is destroyed
                }
            }
            override fun onDoubleClick(v: View?) {
                Log.d("Gesture", "User has tapped twice")
                if(mainUser?.uid != null) {
                    if(!postLikers.contains(mainUser.uid)) {
                        //animate in crown
                        currentItem.postLikers += mainUser.uid
                        animateLikeImageView(holder, mainUser, currentItem)
                    }
                }
            }
        })

        if(mainUser != null) {
            if(postLikers.contains(mainUser.uid)) {
                activatePoints(holder, currentPostLikes)
            } else {
                deactivatePoints(holder)
            }
        } else {
            deactivatePoints(holder)
        }
    }

    private fun animateLikeImageView(holder: FeedViewHolder, mainUser: MemeDomUser, meme: Memes) {
        holder.likeImageView.animate()
            .alpha(1.0f)
            .setDuration(750)
            .withEndAction {
                holder.likeImageView.animate()
                    .alpha(0f)
                    .setDuration(600)
                holder.pointsLayout.visibility = View.VISIBLE
                holder.postUserInfo.visibility = View.VISIBLE
                val updatedPoints = meme.postLikers.count() + 1
                activatePoints(holder, updatedPoints)
                FirestoreHandler().updateArrayDatabaseObject("Memes", meme.postID, mainUser.uid, updatedPoints.toLong())
                FirestoreHandler().updateLikedDatabase(mainUser.uid, meme.postUserUID)
            }
    }

    private fun deactivatePoints(holder: FeedViewHolder) {
        holder.pointsLayout.visibility = View.GONE
        holder.postUserInfo.visibility = View.GONE
    }

    private fun activatePoints(holder: FeedViewHolder, updatedPoints: Int) {
        val appFGColor = Color.parseColor("#FACE0D")

        holder.pointsLayout.visibility = View.VISIBLE
        holder.postUserInfo.visibility = View.VISIBLE
        holder.pointsTextView.text = "${updatedPoints}"
        holder.pointsTextView.setTextColor(appFGColor)
        holder.pointsIcon.setColorFilter(
            ContextCompat.getColor(
                feedAdapterContext,
                R.color.appFGColor
            )
        )
    }

    override fun getItemCount() = feedList.size

    class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val feedImage: ImageButton = itemView.feedImage
        val feedTitle: TextView = itemView.feedTitle
        val feedDate = itemView.feedDate

        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn

        val pointsTextView: TextView = itemView.pointsTextView
        val pointsIcon: ImageView = itemView.pointsIcon
        val pointsLayout: LinearLayout = itemView.pointsLayout

        val postUserInfo = itemView.postUserInfo
        val postProfilePic = itemView.postProfilePic
        val postUserName = itemView.postUsername

        val likeImageView = itemView.likeImageView
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(feedAdapterContext, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        feedAdapterContext.startActivity(intent)
        activity.overridePendingTransition(R.anim.enter_activity, R.anim.enter_activity)
    }
}



