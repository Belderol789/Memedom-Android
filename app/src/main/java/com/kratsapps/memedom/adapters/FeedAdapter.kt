package com.kratsapps.memedom.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.firebase.firestore.FieldValue
import com.kratsapps.memedom.Assets
import com.kratsapps.memedom.CommentsActivity
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.DoubleClickListener
import kotlinx.android.synthetic.main.feed_item.view.*


class FeedAdapter(private var feedList: MutableList<Memes>, private val activity: MainActivity, isMemedom: Boolean): RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    lateinit var feedAdapterContext: Context
    var filteredFeedList = feedList
    var isMemeDom = isMemedom

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        Log.d("HomeFragment", "Filtering with $filteredFeedList")
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.feed_item,
            parent, false
        )
        feedAdapterContext = parent.context
        return FeedViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        //General
        val currentItem = filteredFeedList[position]
        val postUID: String = currentItem.postID

        val mainUser = DatabaseManager(feedAdapterContext).retrieveSavedUser()
        val mainUserID = mainUser?.uid

        val shareCount = if(currentItem.postShares >= 10) "${currentItem.postShares}" else ""
        val commentsCount = if(currentItem.postComments >= 10) "${currentItem.postComments}"  else ""
        var currentPostLikes = currentItem.getPostLikeCount()

        Log.d("Scrolling", "Scrolled through meme ${currentItem.postID}")

        val lp: ConstraintLayout.LayoutParams = holder.feedImage.getLayoutParams() as ConstraintLayout.LayoutParams
        lp.height = (currentItem.postHeight * 1.25).toInt()
        holder.feedImage.setLayoutParams(lp)

        Glide.with(feedAdapterContext)
            .load(currentItem.postImageURL)
            .thumbnail(0.25f)
            .fitCenter()
            .into(holder.feedImage)

        val iconURL = "https://firebasestorage.googleapis.com/v0/b/memedom-fb37b.appspot.com/o/AppSettings%2Fmemedom%20icon.png?alt=media&token=e29c4cae-3a13-47fb-b3c0-0136445a45cf"
        val profilePic = if (currentItem.postProfileURL.isEmpty()) iconURL else currentItem.postProfileURL
        Glide.with(feedAdapterContext)
            .load(profilePic)
            .circleCrop()
            .into(holder.postProfilePic)

        if (currentItem.postTitle.isEmpty()) {
            holder.feedTitle.visibility = View.GONE
        } else {
            holder.feedTitle.visibility = View.VISIBLE
        }
        holder.postUserName.text = currentItem.postUsername
        holder.likeImageView.alpha = 0f

        holder.shareBtn.text = shareCount
        holder.commentsBtn.text = commentsCount
        holder.likeBtn.text = "$currentPostLikes"

        holder.feedTitle.text = currentItem.postTitle
        holder.feedDate.text = currentItem.postDateString()

        holder.reportButton.setOnClickListener {
            if(mainUser != null) { holder.linearReport.visibility = View.VISIBLE }
        }

        holder.cancelBtn.setOnClickListener {
            holder.linearReport.visibility = View.GONE
        }

        holder.reportBtn.setOnClickListener {
            if (mainUser != null) {
                holder.card_view.setBackgroundResource(R.color.errorColor)
                holder.linearReport.visibility = View.GONE
                mainUser.seenOldMemes += postUID
                DatabaseManager(feedAdapterContext).convertUserObject(mainUser!!, "MainUser", {})
            } else {
                activity.showStrangerAlert()
            }
        }

        //Memedom
        if (isMemeDom) {
            val crownImage = feedAdapterContext.resources.getDrawable(R.drawable.ic_action_crown, null)
            holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(crownImage, null, null, null)
            holder.likeImageView.setImageResource(R.drawable.ic_action_crown)
            holder.likeImageView.setColorFilter(ContextCompat.getColor(feedAdapterContext, R.color.appFGColor))
            holder.postUserInfo.visibility = View.VISIBLE
        } //Dating
        else {
            val likeImage = feedAdapterContext.resources.getDrawable(R.drawable.ic_action_like, null)
            holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(likeImage, null, null, null)
            holder.likeImageView.setImageResource(R.drawable.ic_action_like)
            holder.likeImageView.setColorFilter(ContextCompat.getColor(feedAdapterContext, R.color.appDateFGColor))
            holder.postUserInfo.visibility = View.GONE
        }

        holder.commentsBtn.setOnClickListener {
            navigateToComments(currentItem)
        }

        if (mainUser != null) {
            if (currentItem.postLikers.contains(mainUserID)) {
                if ((mainUser.matches.contains(currentItem.postUserUID))) {
                    alreadyLiked(holder, Color.parseColor("#FACE0D"))
                } else if (isMemeDom) {
                    alreadyLiked(holder, Color.parseColor("#58BADC"))
                } else {
                    alreadyLiked(holder, Color.parseColor("#FF69B4"))
                }
            } else {
                didUnlikePost(holder)
            }
        }

        holder.feedImage.setOnClickListener(object : DoubleClickListener() {
            override fun onSingleClick(v: View?) {
                Log.d("Gesture", "User has tapped once")
                navigateToComments(currentItem)
            }

            override fun onDoubleClick(v: View?) {
                Log.d("Gesture", "User has tapped twice")
                if (mainUser?.uid != null) {
                    if (!currentItem.postLikers.contains(mainUser.uid)) {
                        //animate in crown
                        currentItem.postLikers += mainUser.uid
                    }
                } else {
                    activity.showStrangerAlert()
                }
            }
        })

        holder.likeBtn.setOnClickListener {
            if(mainUserID != null) {

                var fieldValue: FieldValue? = null

                if(!currentItem.postLikers.contains(mainUserID)) {
                    Log.d("LikeSystem", "Liking user")
                    //animate in crown
                    fieldValue = FieldValue.arrayUnion(mainUser.uid)
                    currentItem.postLikers += mainUserID
                    mainUser.seenOldMemes += postUID

                    if ((mainUser.matches.contains(currentItem.postUserUID))) {
                        didLikePost(holder, Color.parseColor("#FACE0D"))
                    } else if (isMemeDom) {
                        didLikePost(holder, Color.parseColor("#58BADC"))
                    } else {
                        didLikePost(holder, Color.parseColor("#FF69B4"))
                    }

                    if (isMemeDom) {
                        FirestoreHandler().updateLikeDatabase(mainUserID, currentItem.postUserUID, "liked", feedAdapterContext,1, {})
                    } else {
                        FirestoreHandler().updateLikeDatabase(mainUserID, currentItem.postUserUID, "dating", feedAdapterContext,1, {})
                    }

                    DatabaseManager(feedAdapterContext).convertUserObject(mainUser!!, "MainUser", {})
                } else {
                    fieldValue = FieldValue.arrayRemove(mainUser.uid)
                    currentItem.postLikers -= mainUserID
                    didUnlikePost(holder)
                    Log.d("LikeSystem", "Disliking user")
                }

                val updatedPoints = currentItem.postLikers.count()
                val updatedPointsHash = hashMapOf<String, Any>(
                    "postLikers" to fieldValue,
                    "postPoints" to updatedPoints.toLong()
                )

                FirestoreHandler().updateArrayDatabaseObject(
                    "Memes",
                    currentItem.postID,
                    updatedPointsHash
                )
                holder.likeBtn.text = "${currentItem.postLikers.count()}"
            } else {
                activity.showStrangerAlert()
            }
        }

        holder.shareBtn.setOnClickListener {
            val memeImage = (holder.feedImage.drawable as BitmapDrawable).bitmap
            val shareIntent = Intent(Intent.ACTION_SEND)
            val path = MediaStore.Images.Media.insertImage(
                activity.contentResolver,
                memeImage,
                "Memedom",
                null
            )
            val uri = Uri.parse(path)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/*"
            activity.startActivity(Intent.createChooser(shareIntent, "Spread my Memedom"))
        }

        //Ads
        if (position % 2 == 0) {
            val adRequest = AdRequest.Builder().build()
            holder.mAdView.visibility = View.VISIBLE
            holder.mAdView.loadAd(adRequest)
        } else {
            holder.mAdView.visibility = View.GONE
        }
    }

    private fun alreadyLiked(holder: FeedViewHolder, color: Int) {

        //val color = if (isMemeDom) Color.parseColor("#58BADC") else Color.parseColor("#FF69B4")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            holder.likeBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            holder.shareBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            holder.commentsBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
        }

        holder.likeBtn.setTextColor(color)
        holder.shareBtn.setTextColor(color)
        holder.commentsBtn.setTextColor(color)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun didLikePost(holder: FeedViewHolder, color: Int) {
        holder
            .likeImageView
            .animate()
            .alpha(1.0f)
            .setDuration(750)
            .withEndAction {
                holder.likeImageView.animate().alpha(0.0f)
            }
        alreadyLiked(holder, color)
    }

    private fun didUnlikePost(holder: FeedViewHolder) {
        val color = Color.parseColor("#C0C0C0")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            holder.likeBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            holder.shareBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            holder.commentsBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
        }

        holder.likeBtn.setTextColor(color)
        holder.shareBtn.setTextColor(color)
        holder.commentsBtn.setTextColor(color)
    }

    override fun getItemCount() = filteredFeedList.size

    class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val feedImage: ImageButton = itemView.feedImage
        val feedTitle: TextView = itemView.feedTitle
        val feedDate = itemView.feedDate

        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn
        val likeBtn: Button = itemView.postLikeBtn

        val postUserInfo = itemView.postUserInfo
        val postProfilePic = itemView.postProfilePic
        val postUserName = itemView.postUsername

        val likeImageView = itemView.likeImageView

        val linearReport = itemView.linearReport
        val reportButton = itemView.reportButton
        val reportBtn = itemView.reportBtn
        val cancelBtn = itemView.cancelBtn

        val card_view = itemView.card_view
        val mAdView = itemView.adView
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(feedAdapterContext, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        feedAdapterContext.startActivity(intent)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    fun clear() {
        filteredFeedList.clear()
        notifyDataSetChanged()
    }

    fun addItems(memes: MutableList<Memes>, memedom: Boolean) {
        isMemeDom = memedom
        filteredFeedList = memes
        notifyDataSetChanged()
    }

}



