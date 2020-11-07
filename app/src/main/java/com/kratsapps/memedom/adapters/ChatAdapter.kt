package com.kratsapps.memedom.adapters

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.MemeDomUser
import com.kratsapps.memedom.models.MessageItem
import com.kratsapps.memedom.models.MessageItem.Companion.TYPE_FRIEND_MESSAGE
import com.kratsapps.memedom.models.MessageItem.Companion.TYPE_MY_MESSAGE
import com.kratsapps.memedom.models.MessageViewHolder

class ChatAdapter(var data: MutableList<MessageItem>) : RecyclerView.Adapter<MessageViewHolder<*>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder<*> {
        val chatContext = parent.context
        return when (viewType) {
            TYPE_MY_MESSAGE -> {
                val view = LayoutInflater.from(chatContext).inflate(R.layout.my_message_item, parent, false)
                MyMessageViewHolder(view)
            }
            TYPE_FRIEND_MESSAGE -> {
                val view = LayoutInflater.from(chatContext).inflate(R.layout.friend_message_item, parent, false)
                FriendMessageViewHolder(view, chatContext)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder<*>, position: Int) {
        val item = data[position]
        Log.d("adapter View", position.toString() + item.chatContent)
        when (holder) {
            is MyMessageViewHolder -> holder.bind(item)
            is FriendMessageViewHolder -> holder.bind(item)
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].chatType.toInt()

    class MyMessageViewHolder(val view: View) : MessageViewHolder<MessageItem>(view) {
        private val messageContent = view.findViewById<TextView>(R.id.chatContent)
        private val messageDate = view.findViewById<TextView>(R.id.chatDate)

        override fun bind(item: MessageItem) {
            messageContent.text = item.chatContent
            messageDate.text = item.chatDateString
            messageContent.setTextColor(Color.WHITE)
        }
    }
    class FriendMessageViewHolder(val view: View, context: Context) : MessageViewHolder<MessageItem>(view) {
        private val messageContent = view.findViewById<TextView>(R.id.messageText)
        private val chatDate = view.findViewById<TextView>(R.id.chatDateText)
        private val userImage = view.findViewById<ImageView>(R.id.friend_avatar)
        private val context = context

        override fun bind(item: MessageItem) {
            Glide
                .with(context)
                .load(item.userProfile)
                .circleCrop()
                .into(userImage)

            chatDate.text = item.chatDateString
            messageContent.text = item.chatContent
            messageContent.setTextColor(Color.WHITE)
        }
    }
}