package com.kratsapps.memedom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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
        holder.likesBtn.visibility = View.GONE

        val mainUser = DatabaseManager(feedAdapterContext).retrieveSavedUser()
        val postLikers = currentItem.postLikers
        val appFGColor = Color.parseColor("#FACE0D")

        if(mainUser != null) {
            if(!postLikers.contains(mainUser.uid)) {
                Log.d("Scrolling", "Main user is ${mainUser.uid}")

                holder.imageViewButton.setOnClickListener {

                    Log.d("Firestore", "Post likers $postLikers uid ${mainUser.uid}")

                    val updatedPoints = postLikers.count() + 1

                    holder.postUserInfo.visibility = View.VISIBLE
                    holder.likesBtn.visibility = View.VISIBLE
                    holder.likesBtn.text = "$updatedPoints"
                    holder.likesBtn.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(feedAdapterContext, R.color.appFGColor))
                    holder.likesBtn.setTextColor(appFGColor)

                    FirestoreHandler().updateArrayDatabaseObject("Memes", postUD, mainUser.uid, updatedPoints.toLong())
                    FirestoreHandler().updateLikedDatabase(mainUser.uid, postUserID)
                }
            } else {
                holder.postUserInfo.visibility = View.VISIBLE
                holder.likesBtn.visibility = View.VISIBLE
                holder.likesBtn.text = "${postLikers.count()}"
                holder.likesBtn.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(feedAdapterContext, R.color.appFGColor))
                holder.likesBtn.setTextColor(appFGColor)
            }
        }
    }

    override fun getItemCount() = feedList.size

    class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imageViewButton: ImageButton = itemView.feedImage
        val titleTextView: TextView = itemView.feedTitle

        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn
        val likesBtn: Button = itemView.postLikesButton

        val feedActionLayout = itemView.feedActionLayout
        val postUserInfo = itemView.postUserInfo
    }
}

