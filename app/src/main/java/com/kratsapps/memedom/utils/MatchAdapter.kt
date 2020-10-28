package com.kratsapps.memedom.utils

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
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.Assets
import com.kratsapps.memedom.R
import com.kratsapps.memedom.ReplyActivity
import com.kratsapps.memedom.models.Comments
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.MemeDomUser
import kotlinx.android.synthetic.main.comments_item.view.*
import kotlinx.android.synthetic.main.matches_item.view.*


class MatchAdapter(private val matchList: MutableList<Matches>, private val activity: Activity, private val mainUser: MemeDomUser): RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    lateinit var matchAdapterContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.matches_item,
            parent, false
        )
        matchAdapterContext = parent.context
        return MatchViewHolder(
            itemView
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val currentMatch = matchList[position]

        Log.d("Matches", "Current match ${currentMatch.uid}")

        holder.usernameText.setText(currentMatch.name)
        holder.matchTextView.setText(currentMatch.matchText)
        Glide.with(activity)
            .load(currentMatch.profilePhoto)
            .circleCrop()
            .into(holder.userImage)

        if(!mainUser.matches.contains(currentMatch.uid)) {
            holder.actionLayout.visibility = View.VISIBLE
            holder.matchTextView.visibility = View.GONE
        } else {
            holder.matchTextView.visibility = View.VISIBLE
            holder.actionLayout.visibility = View.GONE
        }
    }

    override fun getItemCount() = matchList.size

    class MatchViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val usernameText = itemView.usernameText
        val userImage = itemView.userImage

        val actionLayout = itemView.actionLayout
        val profileBtn = itemView.profileBtn
        val matchBtn = itemView.matchBtn

        val matchTextView = itemView.matchTextView
    }

}



