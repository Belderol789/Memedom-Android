package com.kratsapps.memedom.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.CommentsActivity
import com.kratsapps.memedom.MainActivity
import com.kratsapps.memedom.R
import com.kratsapps.memedom.firebaseutils.FirestoreHandler
import com.kratsapps.memedom.models.Matches
import com.kratsapps.memedom.models.Memes
import com.kratsapps.memedom.models.Notification
import kotlinx.android.synthetic.main.notification_item.view.*

class NotifAdapter(
    private val notifList: MutableList<Notification>,
    private val activity: MainActivity
): RecyclerView.Adapter<NotifAdapter.NotifViewHolder>() {

    lateinit var notifAdapterContext: Context
    var notifFilterList = notifList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifAdapter.NotifViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.notification_item,
            parent, false
        )
        notifAdapterContext = parent.context
        return NotifViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: NotifAdapter.NotifViewHolder, position: Int) {
        val notifObject = notifFilterList[position]
        holder.notifTitle.setText(notifObject.notifTitle)
        holder.notifText.setText(notifObject.notifText)
        holder.notifDate.setText(notifObject.notifDate())
        Glide.with(activity)
            .load(notifObject.notifPhotoURL)
            .circleCrop()
            .into(holder.notifProfilePhoto)
        holder.notifBtn.setOnClickListener {
            FirestoreHandler().getCommentFromNotif(notifObject.notifContentID, {
                navigateToComments(it)
            })
        }
    }

    override fun getItemCount() = notifFilterList.size

    class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notifTitle = itemView.titleNotifText
        val notifText = itemView.textNotifText
        val notifProfilePhoto = itemView.notifProfileImage
        val notifDate = itemView.notifDateText
        val notifBtn = itemView.notifBtn
    }

    private fun navigateToComments(meme: Memes) {
        val intent: Intent = Intent(notifAdapterContext, CommentsActivity::class.java)
        intent.putExtra("CommentMeme", meme)
        intent.putExtra("isMemeDom", true)
        notifAdapterContext.startActivity(intent)
        activity.overridePendingTransition(
            R.anim.enter_activity,
            R.anim.enter_activity
        )
    }

    fun clear() {
        notifFilterList.clear()
        notifyDataSetChanged()
    }

    fun addItems(notifs: MutableList<Notification>) {
        notifFilterList.addAll(notifs)
        notifyDataSetChanged()
    }

}