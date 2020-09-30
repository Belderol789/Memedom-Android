package com.kratsapps.memedom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.feed_item.view.*

class FeedAdapter(private val feedList: List<Memes>): RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    lateinit var feedAdapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.feed_item,
        parent, false)
        feedAdapterContext = parent.context
        return FeedViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val currentItem = feedList[position]
        val postUD: String = currentItem.postID
        val postUserID: String = currentItem.postUserUID
        var currentPostLikes = currentItem.getPostLikeCount()

        Glide.with(feedAdapterContext)
            .load(currentItem.postImageURL)
            .centerCrop()
            .into(holder.imageView)
        holder.titleTextView.text = currentItem.postTitle
        holder.likeBtn.text = "$currentPostLikes"

        var mainUserID = DatabaseManager(feedAdapterContext).getMainUserID()

        if(mainUserID != null) {
            holder.likeBtn.setOnClickListener {
                holder.likeBtn.text = "$currentPostLikes"
                FirestoreHandler().updateArrayDatabaseObject("Memes", postUD, mainUserID)
            }
        }
    }

    override fun getItemCount() = feedList.size

    class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.feedImage
        val titleTextView: TextView = itemView.feedTitle
        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn
        val likeBtn: Button = itemView.postLikeBtn
    }
}

