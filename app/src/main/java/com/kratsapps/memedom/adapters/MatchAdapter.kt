package com.kratsapps.memedom.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
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
import kotlinx.android.synthetic.main.matches_item.view.*
import java.util.*


class MatchAdapter(
    private val matchList: MutableList<Matches>,
    private val activity: MainActivity
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>(),
    Filterable {
    companion object {
        const val START_CHAT_REQUEST_CODE = 69
    }

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
        if (currentMatch.chatDate != 0L) {
            holder.chatDate.setText(currentMatch.postDateString(currentMatch.chatDate))
        }

        if (currentMatch.onlineDate != 0L) {
            val onlineDateString = "Last online: ${currentMatch.postDateString(currentMatch.onlineDate)}"
            holder.onlineDate.setText(onlineDateString)
        }

        if (currentMatch.matchStatus == true) {
            holder.actionLayout.visibility = View.GONE
            holder.matchTextView.visibility = View.VISIBLE
            holder.chatBtn.visibility = View.VISIBLE
        } else {
            holder.actionLayout.visibility = View.VISIBLE
            holder.matchTextView.visibility = View.GONE
            holder.chatBtn.visibility = View.GONE
        }

        if (currentMatch.online) {
            holder.onlineStatus.visibility = View.VISIBLE
        } else {
            holder.onlineStatus.visibility = View.INVISIBLE
        }

        Glide.with(activity)
            .load(currentMatch.profilePhoto)
            .circleCrop()
            .into(holder.userImage)

        holder.profileBtn.setOnClickListener {
            val intent: Intent = Intent(activity, ProfileActivity::class.java)
            intent.putExtra("MatchUser", currentMatch)
            intent.putExtra("isMatching", true)
            activity.startActivity(intent)
        }

        holder.rejectBtn.setOnClickListener {
            val memeDomUser = MemeDomUser()
            memeDomUser.uid = currentMatch.uid
            FirestoreHandler().rejectUser(memeDomUser, matchAdapterContext)
            removeRow(position)
        }

        holder.chatBtn.setOnClickListener {
            goToChat(currentMatch)
        }

        holder.matchBtn.setOnClickListener {
            // Go to Chat
            val data = hashMapOf<String, Any>(
                "matchStatus" to true,
                "chatDate" to System.currentTimeMillis(),
                "onlineDate" to System.currentTimeMillis()
            )
            FirestoreHandler().updateMatch(currentMatch.uid, data, matchAdapterContext, {})
            FirestoreHandler().updateUserLiked(currentMatch.uid, matchAdapterContext, {
                goToChat(currentMatch)
            })
        }
    }

    private fun goToChat(currentMatch: Matches) {
        val intent: Intent = Intent(activity, ChatActivity::class.java)
        val chatUser = MemeDomUser()
        chatUser.name = currentMatch.name
        chatUser.profilePhoto = currentMatch.profilePhoto
        chatUser.uid = currentMatch.uid
        intent.putExtra("ChatUser", chatUser)
        activity.startActivityForResult(intent, START_CHAT_REQUEST_CODE)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    override fun getItemCount() = matchFilterList.size

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText = itemView.usernameText
        val chatDate = itemView.chatDate
        val onlineDate = itemView.onlineDate
        val userImage = itemView.userImage

        val actionLayout = itemView.actionLayout
        val profileBtn = itemView.profileBtn
        val matchBtn = itemView.matchBtn
        val chatBtn = itemView.chatBtn
        val rejectBtn = itemView.rejectBtn

        val matchTextView = itemView.matchTextView
        val onlineStatus = itemView.onlineStatus
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

    fun removeRow(position: Int) {
        matchFilterList.removeAt(position)
        notifyItemRemoved(position)
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



