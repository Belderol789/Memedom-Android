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
import com.kratsapps.memedom.R
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.utils.DatabaseManager
import com.kratsapps.memedom.utils.DoubleClickListener
import kotlinx.android.synthetic.main.feed_item.view.*


class FeedAdapter(private var feedList: MutableList<Memes>, private val activity: Activity, val isProfile: Boolean, isMemedom: Boolean): RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

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
        val currentItem = filteredFeedList[position]
        val postUID: String = currentItem.postID

        val mainUser = DatabaseManager(feedAdapterContext).retrieveSavedUser()
        val mainUserID = mainUser?.uid

        val shareCount = if(currentItem.postShares >= 10) "${currentItem.postShares}" else ""
        val commentsCount = if(currentItem.postComments >= 10) "${currentItem.postComments}"  else ""

        var currentPostLikes = currentItem.getPostLikeCount()

        Log.d("Scrolling", "Scrolled through meme ${currentItem.postID}")

        val lp: ConstraintLayout.LayoutParams = holder.feedImage.getLayoutParams() as ConstraintLayout.LayoutParams
        lp.height = (currentItem.postHeight * 1.5).toInt()
        holder.feedImage.setLayoutParams(lp)

        Glide.with(feedAdapterContext)
            .load(currentItem.postImageURL)
            .thumbnail(0.25f)
            .fitCenter()
            .into(holder.feedImage)

        Log.d("ProfileURL", "ProfilePhotoItem ${currentItem.postProfileURL}")

        Glide.with(feedAdapterContext)
            .load(currentItem.postProfileURL)
            .circleCrop()
            .error(
                ContextCompat.getDrawable(
                    activity.applicationContext,
                    R.drawable.ic_action_name
                )
            )
            .into(holder.postProfilePic)

        if (isMemeDom) {
            holder.likeBtn.setImageResource(R.drawable.ic_action_crown)
            holder.likeImageView.setImageResource(R.drawable.ic_action_crown)
            holder.pointsIcon.setImageResource(R.drawable.ic_action_crown)
            holder.likeImageView.setColorFilter(
                ContextCompat.getColor(feedAdapterContext, R.color.appFGColor)
            )
        } else {
            holder.likeBtn.setImageResource(R.drawable.ic_action_like)
            holder.likeImageView.setImageResource(R.drawable.ic_action_like)
            holder.pointsIcon.setImageResource(R.drawable.ic_action_like)
            holder.likeImageView.setColorFilter(
                ContextCompat.getColor(feedAdapterContext, R.color.appDateFGColor)
            )
        }

        holder.postUserName.text = currentItem.postUsername
        holder.likeImageView.alpha = 0f

        holder.shareBtn.text = shareCount
        holder.commentsBtn.text = commentsCount
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
            }
        }

        holder.likeBtn.setOnClickListener {
            if(mainUserID != null) {
                if(!currentItem.postLikers.contains(mainUserID)) {
                    //animate in crown
                    currentItem.postLikers += mainUserID
                    animateLikeImageView(holder, mainUser, currentItem)
                    mainUser.seenOldMemes += postUID

                    val updatedPoints = currentItem.postLikers.count() + 1
                    val fieldValue: FieldValue = FieldValue.arrayUnion(mainUser.uid)
                    val updatedPointsHash = hashMapOf<String, Any>(
                        "postLikers" to fieldValue,
                        "postPoints" to updatedPoints.toLong()
                    )

                    FirestoreHandler().updateArrayDatabaseObject(
                        "Memes",
                        currentItem.postID,
                        updatedPointsHash
                    )

                    if (isMemeDom) {
                        FirestoreHandler().updateLikeDatabase(mainUserID, currentItem.postUserUID, "liked", feedAdapterContext,1, {})
                    } else {
                        FirestoreHandler().updateLikeDatabase(mainUserID, currentItem.postUserUID, "dating", feedAdapterContext,1, {})
                    }

                    DatabaseManager(feedAdapterContext).convertUserObject(mainUser!!, "MainUser", {})
                }
            } 
        }

        holder.commentsBtn.setOnClickListener {
            navigateToComments(currentItem)
        }

        holder.feedImage.setOnClickListener(object : DoubleClickListener() {
            override fun onSingleClick(v: View?) {
                Log.d("Gesture", "User has tapped once")
                if (mainUser?.uid != null && currentItem.postLikers.contains(mainUser.uid)) {
                    navigateToComments(currentItem)
                    //make sure when user logs out, all data is destroyed
                }
            }

            override fun onDoubleClick(v: View?) {
                Log.d("Gesture", "User has tapped twice")
                if (mainUser?.uid != null) {
                    if (!currentItem.postLikers.contains(mainUser.uid)) {
                        //animate in crown
                        currentItem.postLikers += mainUser.uid
                        animateLikeImageView(holder, mainUser, currentItem)
                    }
                }
            }
        })

        if(mainUser != null) {

            Log.d(
                "Matches",
                "Current matches ${mainUser.matches} currentItem ${currentItem.postLikers}"
            )

            if (mainUser.matches.contains(currentItem.postUserUID)) {
                activatePoints(holder, currentPostLikes, Assets().specialColor, R.color.specialColor)
            } else if(currentItem.postLikers.contains(mainUser.uid)) {
                if (isMemeDom) {
                    activatePoints(holder, currentPostLikes, Assets().appFGColor, R.color.appFGColor)
                } else  {
                    activatePoints(holder, currentPostLikes, Assets().appDateFGColor, R.color.appDateFGColor)
                }
            } else {
                deactivatePoints(holder)
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
        } else {
            deactivatePoints(holder)
        }

        if (isProfile) {
            holder.mAdView.visibility = View.GONE
            holder.reportButton.visibility = View.INVISIBLE
        } else {
            if (position % 2 == 0) {
                val adRequest = AdRequest.Builder().build()
                holder.mAdView.loadAd(adRequest)
            } else {
                holder.mAdView.visibility = View.GONE
            }
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
                if (isMemeDom) {
                    activatePoints(holder, updatedPoints, Assets().appFGColor, R.color.appFGColor)
                } else {
                    activatePoints(holder, updatedPoints, Assets().appDateFGColor, R.color.appDateFGColor)
                }
            }
    }

    private fun deactivatePoints(holder: FeedViewHolder) {
        holder.pointsLayout.visibility = View.GONE
        holder.postUserInfo.visibility = View.GONE
        holder.likeBtn.visibility = View.VISIBLE

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            holder.shareBtn.setTextColor(Assets().appFGColor)
            holder.commentsBtn.setTextColor(Assets().appFGColor)
            holder.shareBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#C0C0C0")))
            holder.commentsBtn.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#C0C0C0")))
        }
    }

    private fun activatePoints(holder: FeedViewHolder, updatedPoints: Int, color: Int, rColor: Int) {
        holder.pointsLayout.visibility = View.VISIBLE
        holder.postUserInfo.visibility = View.VISIBLE
        holder.likeBtn.visibility = View.GONE
        holder.pointsTextView.text = "${updatedPoints}"
        holder.pointsTextView.setTextColor(color)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            holder.shareBtn.setTextColor(color)
            holder.commentsBtn.setTextColor(color)
            holder.shareBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
            holder.commentsBtn.setCompoundDrawableTintList(ColorStateList.valueOf(color))
        }
        holder.pointsTextView.setTextColor(color)
        holder.pointsIcon.setColorFilter(
            ContextCompat.getColor(feedAdapterContext, rColor)
        )
    }

    override fun getItemCount() = filteredFeedList.size

    class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val feedImage: ImageButton = itemView.feedImage
        val feedTitle: TextView = itemView.feedTitle
        val feedDate = itemView.feedDate

        val shareBtn: Button = itemView.postShareBtn
        val commentsBtn: Button = itemView.postCommentsBtn
        val likeBtn: ImageButton = itemView.postLikeBtn

        val pointsTextView: TextView = itemView.pointsTextView
        val pointsIcon: ImageView = itemView.pointsIcon
        val pointsLayout: LinearLayout = itemView.pointsLayout

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



