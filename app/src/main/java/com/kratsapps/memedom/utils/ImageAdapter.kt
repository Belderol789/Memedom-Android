package com.kratsapps.memedom.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.R
import kotlinx.android.synthetic.main.image_cell.view.*


class ImageAdapter(private val imageList: List<String>, private val activity: Activity): RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    lateinit var adapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.image_cell, parent, false)
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
    }

    override fun getItemCount() = imageList.size

    class ImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imageCell = itemView.imageCell
    }
}



