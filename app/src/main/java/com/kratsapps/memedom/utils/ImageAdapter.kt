package com.kratsapps.memedom.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.R
import com.kratsapps.memedom.fragments.ProfileFragment
import kotlinx.android.synthetic.main.image_cell.view.*


class ImageAdapter(private val imageList: MutableList<String>, private val activity: Activity, private val fragment: ProfileFragment): RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    lateinit var adapterContext: Context

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
            .into(holder.imageCell)

        holder.deleteBtn.setOnClickListener {
            removeAt(position)
            fragment.showSave()
        }
    }

    override fun getItemCount() = imageList.size

    class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imageCell = itemView.imageCell
        val deleteBtn = itemView.deleteButton
    }

    fun removeAt(position: Int) {
        Log.d("ImageList", "Count before ${imageList.count()}")
        imageList.removeAt(position)

        notifyItemRemoved(position)
        notifyItemRangeChanged(position, imageList.count())
        Log.d("ImageList", "Count after ${imageList.count()}")
    }
}



