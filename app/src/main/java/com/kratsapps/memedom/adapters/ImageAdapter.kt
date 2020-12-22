package com.kratsapps.memedom.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.*
import com.kratsapps.memedom.fragments.ProfileFragment
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import kotlinx.android.synthetic.main.image_cell.view.*


class ImageAdapter(private val imageList: MutableList<String>, private val memeList: MutableList<Memes>?, private val activity: Activity, private val fragment: ProfileFragment?, isMemes: Boolean): RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    lateinit var adapterContext: Context
    val isMeme = isMemes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(
            R.layout.image_cell,
            parent,
            false
        )
        val lp = v.layoutParams
        lp.height = parent.measuredWidth / 3
        lp.width = parent.measuredWidth / 3
        v.layoutParams = lp
        return ImageViewHolder(v)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(activity)
            .load(imageList[position])
            .centerCrop()
            .thumbnail(0.25f)
            .into(holder.imageCell)

        if (activity is MainActivity) {
            holder.deleteBtn.visibility = View.GONE
            holder.imageCell.setOnClickListener {
                Log.d("ProfileActivity", "IsMemes $isMeme")
                if (isMeme && memeList != null) {
                    navigateToComments(memeList[position])
                } else if (!isMeme && position != 0) {
                    navigateToLargeImage(imageList[position])
                } else if (position == 0 && fragment != null) {
                    fragment.profilePhotoSelected = false
                    fragment.openImageGallery()
                }
            }
            if (!isMeme) {
                Log.d("ProfileActivity", "Position $position")
                if (position == 0) {
                    holder.imageCell.setColorFilter(Color.parseColor("#111111"))
                } else {
                    holder.deleteBtn.visibility = View.VISIBLE
                    holder.deleteBtn.setOnClickListener {
                        removeAt(position)
                    }
                }
            }
        } else if (activity is MemedomActivity) {
            holder.imageCell.setOnClickListener {
                (activity as MemedomActivity).didSelectCurrentImage(position)
            }
            holder.deleteBtn.visibility = View.GONE
        } else {
            holder.deleteBtn.visibility = View.GONE
        }
    }

    override fun getItemCount() = imageList.size

    class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imageCell = itemView.imageCell
        val deleteBtn = itemView.deleteButton
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(activity, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        activity.startActivity(intent)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    private fun navigateToLargeImage(imageURI: String) {
        val intent: Intent = Intent(activity, ImageActivity::class.java)
        intent.putExtra("EnlargeImageURL", imageURI)
        activity.startActivity(intent)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    fun removeAt(position: Int) {
        if (fragment != null) {
            fragment.removeGalleryItem(position)
            imageList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imageList.count())
        }
    }
}



