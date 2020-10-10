package com.kratsapps.memedom

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
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.FirestoreHandler
import kotlinx.android.synthetic.main.feed_item.view.*

class FeedAdapter(private val feedList: List<Memes>, private val activity: Activity): RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    lateinit var feedAdapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.feed_item,
        parent, false)
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
        holder.feedImage.setOnClickListener{
            navigateToComments(currentItem)
        }
        holder.commentsBtn.setOnClickListener {
            navigateToComments(currentItem)
        }
        holder.feedTitle.text = currentItem.postTitle

        Glide.with(feedAdapterContext)
            .load(currentItem.postProfileURL)
            .centerCrop()
            .into(holder.postProfilePic)
        holder.feedDate.text = currentItem.postDateString()
        holder.pointsLayout.visibility = View.GONE

        holder

        val mainUser = DatabaseManager(feedAdapterContext).retrieveSavedUser()
        val postLikers = currentItem.postLikers

        if(mainUser != null) {
            if(!postLikers.contains(mainUser.uid)) {
                Log.d("Scrolling", "Main user is ${mainUser.uid}")

                holder.feedImage.setOnClickListener {

                    Log.d("Firestore", "Post likers $postLikers uid ${mainUser.uid}")

                    val updatedPoints = postLikers.count() + 1
                    activatePoints(holder, updatedPoints)

                    FirestoreHandler().updateArrayDatabaseObject("Memes", postUD, mainUser.uid, updatedPoints.toLong())
                    FirestoreHandler().updateLikedDatabase(mainUser.uid, postUserID)
                }
            } else {
                activatePoints(holder, postLikers.count())
            }
        }
    }

    private fun activatePoints(holder: FeedViewHolder, updatedPoints: Int) {
        val appFGColor = Color.parseColor("#FACE0D")

        holder.pointsLayout.visibility = View.VISIBLE
        holder.postUserInfo.visibility = View.VISIBLE
        holder.pointsTextView.text = "${updatedPoints}"
        holder.pointsTextView.setTextColor(appFGColor)
        holder.pointsIcon.setColorFilter(ContextCompat.getColor(feedAdapterContext, R.color.appFGColor))
    }

    override fun getItemCount() = feedList.size

    class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val feedImage: ImageButton = itemView.feedImage
        val feedTitle: TextView = itemView.feedTitle
        val feedDate = itemView.feedDate

        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn
        val feedActionLayout = itemView.feedActionLayout

        val pointsTextView: TextView = itemView.pointsTextView
        val pointsIcon: ImageView = itemView.pointsIcon
        val pointsLayout: LinearLayout = itemView.pointsLayout

        val postUserInfo = itemView.postUserInfo
        val postProfilePic = itemView.postProfilePic
        val postUserName = itemView.postUsername
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(feedAdapterContext, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        feedAdapterContext.startActivity(intent)
        activity.overridePendingTransition(R.anim.enter_activity, R.anim.enter_activity)
    }
}



