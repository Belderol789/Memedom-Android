package com.kratsapps.memedom

import android.content.Context
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

class FeedAdapter(private val feedList: List<Memes>): RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

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
            .into(holder.imageViewButton)
        holder.titleTextView.text = currentItem.postTitle
        holder.pointsLayout.visibility = View.GONE

        val mainUser = DatabaseManager(feedAdapterContext).retrieveSavedUser()
        val postLikers = currentItem.postLikers

        if(mainUser != null) {
            if(!postLikers.contains(mainUser.uid)) {
                Log.d("Scrolling", "Main user is ${mainUser.uid}")

                holder.imageViewButton.setOnClickListener {

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
        val imageViewButton: ImageButton = itemView.feedImage
        val titleTextView: TextView = itemView.feedTitle

        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn

        val pointsTextView: TextView = itemView.pointsTextView
        val pointsIcon: ImageView = itemView.pointsIcon
        val pointsLayout: LinearLayout = itemView.pointsLayout

        val feedActionLayout = itemView.feedActionLayout
        val postUserInfo = itemView.postUserInfo
    }

}



