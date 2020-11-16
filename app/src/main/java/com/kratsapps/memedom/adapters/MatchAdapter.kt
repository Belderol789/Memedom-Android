package com.kratsapps.memedom.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.FieldClassification
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Filter
import android.widget.Filterable
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.*
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.Memes
import kotlinx.android.synthetic.main.matches_item.view.*
import java.util.*


class MatchAdapter(
    private val matchList: MutableList<Matches>,
    private val activity: MainActivity,
    private val mainUser: MemeDomUser
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>(),
    Filterable {

    lateinit var matchAdapterContext: Context
    var matchFilterList = matchList


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.matches_item,
            parent, false
        )
        matchAdapterContext = parent.context
        return MatchViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val currentMatch = matchFilterList[position]

        Log.d("Matching-Fragment", "Current match ${currentMatch.uid} FilterList $matchFilterList")

        holder.usernameText.setText(currentMatch.name)

        var matchText = ""
        if (URLUtil.isValidUrl(currentMatch.matchText)) {
            matchText = "Sent an Image"
        } else if (currentMatch.matchText.isEmpty()) {
            matchText = "Send your first message!"
        } else {
            matchText = currentMatch.matchText
        }
        holder.matchTextView.setText(matchText)
        holder.matchDate.setText(currentMatch.postDateString())

        Glide.with(activity)
            .load(currentMatch.profilePhoto)
            .circleCrop()
            .into(holder.userImage)

        if (!mainUser.matches.contains(currentMatch.uid)) {
            holder.actionLayout.visibility = View.VISIBLE
            holder.matchTextView.visibility = View.GONE
            holder.chatBtn.visibility = View.GONE
        } else {
            holder.matchTextView.visibility = View.VISIBLE
            holder.matchBtn.visibility = View.VISIBLE
            holder.actionLayout.visibility = View.GONE
        }

        holder.profileBtn.setOnClickListener {
            val intent: Intent = Intent(matchAdapterContext, ProfileActivity::class.java)
            intent.putExtra("MatchUser", currentMatch)
            activity.startActivityForResult(intent, 999)
        }

        holder.chatBtn.setOnClickListener {
            goToChat(currentMatch)
        }

        holder.matchBtn.setOnClickListener {
            FirestoreHandler().updateUserLiked(currentMatch.uid, matchAdapterContext)
            FirestoreHandler().updateMatch(currentMatch.uid, matchAdapterContext)
            goToChat(currentMatch)
            // Go to Chat
        }

    }

    private fun goToChat(currentMatch: Matches) {
        val intent: Intent = Intent(activity, ChatActivity::class.java)
        val chatUser = MemeDomUser()
        chatUser.name = currentMatch.name
        chatUser.profilePhoto = currentMatch.profilePhoto
        chatUser.uid = currentMatch.uid
        intent.putExtra("ChatUser", chatUser)
        activity.startActivity(intent)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    override fun getItemCount() = matchFilterList.size

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText = itemView.usernameText
        val matchDate = itemView.matchDate
        val userImage = itemView.userImage

        val actionLayout = itemView.actionLayout
        val profileBtn = itemView.profileBtn
        val matchBtn = itemView.matchBtn
        val chatBtn = itemView.chatBtn

        val matchTextView = itemView.matchTextView
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                if (charSearch.isEmpty()) {
                    matchFilterList = matchList
                } else {
                    val resultList = mutableListOf<Matches>()
                    for (row in matchList) {
                        if (row.name.toLowerCase(Locale.ROOT)
                                .contains(charSearch.toLowerCase(Locale.ROOT))
                        ) {
                            resultList.add(row)
                        }
                    }
                    matchFilterList = resultList
                }
                val filterResults = FilterResults()
                filterResults.values = matchFilterList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                matchFilterList = results?.values as MutableList<Matches>
                notifyDataSetChanged()
            }
        }
    }

    fun clear() {
        matchFilterList.clear()
        notifyDataSetChanged()
    }

    fun addItems(matches: MutableList<Matches>) {
        matchFilterList = matches
        notifyDataSetChanged()
    }
}



